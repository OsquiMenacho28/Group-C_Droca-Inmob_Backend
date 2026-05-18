package com.inmobiliaria.visit_calendar_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inmobiliaria.visit_calendar_service.client.PersonClient;
import com.inmobiliaria.visit_calendar_service.client.PropertyClient;
import com.inmobiliaria.visit_calendar_service.domain.InteractionType;
import com.inmobiliaria.visit_calendar_service.dto.VisitCalendarDTOs.*;
import com.inmobiliaria.visit_calendar_service.dto.response.PersonResponse;
import com.inmobiliaria.visit_calendar_service.dto.response.PropertyResponse;
import com.inmobiliaria.visit_calendar_service.exception.ResourceNotFoundException;
import com.inmobiliaria.visit_calendar_service.exception.ScheduleConflictException;
import com.inmobiliaria.visit_calendar_service.model.CalendarEvent;
import com.inmobiliaria.visit_calendar_service.model.Visit;
import com.inmobiliaria.visit_calendar_service.repository.CalendarEventRepository;
import com.inmobiliaria.visit_calendar_service.repository.VisitRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de lógica de negocio para el calendario de visitas.
 *
 * <p>HU1: Visualizar calendario compartido con filtros (agente, fecha, propiedad). HU2: Programar
 * visita validando disponibilidad y detectando conflictos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

  private final CalendarEventRepository calendarEventRepository;
  private final PropertyClient propertyClient;
  private final NotificationService notificationService;
  private final PersonClient personClient;
  private final VisitRepository visitRepository;
  private final ObjectMapper objectMapper;

  // =====================================================================
  // HU1: GET /calendar — Visualizar calendario compartido del equipo
  // =====================================================================

  /**
   * PA1: Retorna todos los eventos del calendario en un rango de fechas. Los eventos del agente
   * autenticado se marcan con ownEvent=true (para resaltarlos visualmente). PA3: Si se pasa
   * propertyId, filtra solo por esa propiedad.
   *
   * @param requestingAgentId ID del agente autenticado (para marcar sus propios eventos)
   * @param from Inicio del rango de fechas
   * @param to Fin del rango de fechas
   * @param agentId Filtro opcional por agente específico
   * @param propertyId Filtro opcional por propiedad específica
   */
  public CalendarResponse getCalendar(
      String requestingAgentId, Instant from, Instant to, String agentId, String propertyId) {

    log.debug(
        "Obteniendo calendario: agenteFiltro={}, propiedadFiltro={}, desde={}, hasta={}",
        agentId,
        propertyId,
        from,
        to);

    List<CalendarEvent> events;

    if (propertyId != null && !propertyId.isBlank()) {
      // PA3: Filtro por propiedad específica
      events = calendarEventRepository.findByPropertyIdAndDateRange(propertyId, from, to);
    } else if (agentId != null && !agentId.isBlank()) {
      // Filtro por agente específico
      events = calendarEventRepository.findByAgentIdAndDateRange(agentId, from, to);
    } else {
      // Vista completa del equipo
      events = calendarEventRepository.findByDateRange(from, to);
    }

    List<Visit> responses =
        events.stream()
            .map(event -> toResponse(event, requestingAgentId))
            .collect(Collectors.toList());

    long myEventsCount = responses.stream().filter(v -> v.getOwnEvent()).count();

    return CalendarResponse.builder()
        .events(responses)
        .from(from)
        .to(to)
        .totalEvents(responses.size())
        .myEvents((int) myEventsCount)
        .build();
  }

  // =====================================================================
  // HU2: POST /visits — Programar visita con validación de conflictos
  // =====================================================================

  /**
   * PA2: Valida si existe conflicto de horario ANTES de crear el evento. Retorna detalles del
   * conflicto y una sugerencia de horario alternativo.
   */
  public ConflictResponse checkConflict(String propertyId, Instant startTime, Instant endTime) {
    validateDateRange(startTime, endTime);

    List<CalendarEvent> conflicts =
        calendarEventRepository.findConflictingEventsForNew(propertyId, startTime, endTime);

    if (conflicts.isEmpty()) {
      return ConflictResponse.builder()
          .hasConflict(false)
          .message("El horario está disponible")
          .conflictingEvents(List.of())
          .build();
    }

    // Sugerir horario después del último evento conflictivo
    Instant suggestedStart =
        conflicts.stream()
            .map(CalendarEvent::getEndTime)
            .max(Instant::compareTo)
            .orElse(endTime)
            .plus(30, ChronoUnit.MINUTES);

    long durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
    Instant suggestedEnd = suggestedStart.plus(durationMinutes, ChronoUnit.MINUTES);

    return ConflictResponse.builder()
        .hasConflict(true)
        .message(
            "Ya existe una visita programada para este inmueble en ese horario. "
                + "Por favor selecciona otro horario.")
        .conflictingEvents(
            conflicts.stream().map(e -> toResponse(e, null)).collect(Collectors.toList()))
        .suggestedStartTime(suggestedStart)
        .suggestedEndTime(suggestedEnd)
        .build();
  }

  /**
   * PA1 + PA2 + PA3 de HU2: Crea un nuevo evento de visita en el calendario. Lanza
   * ScheduleConflictException si ya existe un conflicto de horario. La visita aparece
   * automáticamente en el calendario compartido (PA1).
   */
  public Visit createVisit(CreateVisitRequest request) {
    validateDateRange(request.getStartTime(), request.getEndTime());

    // Verificar conflictos de horario (PA2)
    List<CalendarEvent> conflicts =
        calendarEventRepository.findConflictingEventsForNew(
            request.getPropertyId(), request.getStartTime(), request.getEndTime());

    if (!conflicts.isEmpty()) {
      throw new ScheduleConflictException(
          "Ya existe una visita programada para el inmueble '"
              + request.getPropertyName()
              + "' en ese horario. "
              + "Por favor selecciona otro horario.");
    }

    PropertyResponse property = propertyClient.getPropertyById(request.getPropertyId());
    if (property != null && property.ownerId() != null) {
      PersonResponse owner = personClient.getPersonByAuthUserId(property.ownerId());
      if (owner != null && owner.email() != null) {
        notificationService.notifyPropertyOwner(owner.email(), owner.fullName(), request);
      }
    }

    CalendarEvent event =
        CalendarEvent.builder()
            .propertyId(request.getPropertyId())
            .propertyName(request.getPropertyName())
            .propertyAddress(request.getPropertyAddress())
            .agentId(request.getAgentId())
            .agentName(request.getAgentName())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .type(CalendarEvent.EventType.VISIT)
            .status(CalendarEvent.EventStatus.SCHEDULED)
            .notes(request.getNotes())
            .createdAt(Instant.now())
            .build();

    CalendarEvent saved = calendarEventRepository.save(event);
    log.info(
        "Visita creada exitosamente: id={}, propiedad={}, agente={}, inicio={}",
        saved.getId(),
        saved.getPropertyName(),
        saved.getAgentName(),
        saved.getStartTime());

    notifyOwnerAboutVisit(saved, "schedule");

    return toResponse(saved, request.getAgentId());
  }

  /** PA3 de HU2: Obtiene la agenda del día para un agente específico. */
  public List<Visit> getAgentDayAgenda(String agentId, Instant day) {
    Instant dayStart = day.truncatedTo(ChronoUnit.DAYS);
    Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.NANOS);
    List<CalendarEvent> events =
        calendarEventRepository.findByDayAndAgent(dayStart, dayEnd, agentId);
    return events.stream().map(e -> toResponse(e, agentId)).collect(Collectors.toList());
  }

  /**
   * Obtiene todas las visitas de una propiedad específica. Usado por el frontend para mostrar el
   * historial de visitas en el detalle del inmueble.
   */
  public List<Visit> getVisitsByProperty(String propertyId) {
    log.debug("Obteniendo historial de visitas para la propiedad: {}", propertyId);
    return visitRepository.findByPropertyId(propertyId);
  }

  /** Obtiene un evento por ID. */
  public Visit getById(String id, String requestingAgentId) {
    CalendarEvent event =
        calendarEventRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + id));
    return toResponse(event, requestingAgentId);
  }

  /** Cancela un evento (solo el agente dueño puede cancelar el suyo). */
  public Visit cancelEvent(String id, String agentId) {
    CalendarEvent event =
        calendarEventRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + id));

    if (!event.getAgentId().equals(agentId)) {
      throw new IllegalArgumentException("Solo el agente responsable puede cancelar esta visita");
    }

    event.setStatus(CalendarEvent.EventStatus.CANCELLED);
    CalendarEvent saved = calendarEventRepository.save(event);
    log.info("Visita cancelada: id={}", id);

    notifyOwnerAboutVisit(saved, "cancel");

    return toResponse(saved, agentId);
  }

  // =====================================================================
  // Helpers
  // =====================================================================

  private void notifyOwnerAboutVisit(CalendarEvent event, String action) {
    try {
      String raw = propertyClient.getPropertyRaw(event.getPropertyId());
      JsonNode root = objectMapper.readTree(raw);
      JsonNode dataNode = root.get("data");
      if (dataNode == null) {
        log.warn("No data node in response for property {}", event.getPropertyId());
        return;
      }
      PropertyResponse property = objectMapper.treeToValue(dataNode, PropertyResponse.class);
      if (property == null || property.ownerId() == null) {
        log.warn("Property {} has no owner", event.getPropertyId());
        return;
      }
      if (property == null || property.ownerId() == null) {
        log.debug("Property or owner not found for notification: {}", event.getPropertyId());
        return;
      }
      String ownerId = property.ownerId();
      // Obtener nombre del propietario
      PersonResponse owner = personClient.getPersonByAuthUserId(ownerId);
      String ownerName =
          (owner != null && owner.fullName() != null) ? owner.fullName() : "Propietario";

      String subject;
      String content;
      InteractionType type;
      Map<String, Object> details =
          Map.of(
              "propertyId", event.getPropertyId(),
              "propertyName", event.getPropertyName(),
              "visitId", event.getId(),
              "visitStartTime", event.getStartTime().toString(),
              "agentName", event.getAgentName());

      if ("schedule".equals(action)) {
        subject = "Nueva visita agendada para tu propiedad";
        content =
            String.format(
                "El agente %s ha agendado una visita para tu propiedad '%s' el día %s.",
                event.getAgentName(), event.getPropertyName(), event.getStartTime());
        type = InteractionType.PROPIEDAD_MOD;
      } else {
        subject = "Visita cancelada para tu propiedad";
        content =
            String.format(
                "El agente %s ha cancelado la visita programada para tu propiedad '%s' el día %s.",
                event.getAgentName(), event.getPropertyName(), event.getStartTime());
        type = InteractionType.PROPIEDAD_MOD;
      }

      notificationService.sendInAppNotificationToOwner(
          ownerId, ownerName, event.getPropertyName(), subject, content, type, details);
    } catch (Exception e) {
      log.warn(
          "Could not send in-app notification to owner for visit {}: {}",
          event.getId(),
          e.getMessage());
    }
  }

  private void validateDateRange(Instant start, Instant end) {
    if (start == null || end == null) {
      throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
    }
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
    }
    if (start.isBefore(Instant.now())) {
      throw new IllegalArgumentException("No se puede programar una visita en el pasado");
    }
  }

  private Visit toResponse(CalendarEvent event, String requestingAgentId) {
    Visit visit = new Visit();
    visit.setId(event.getId());
    visit.setPropertyId(event.getPropertyId());
    visit.setPropertyName(event.getPropertyName());
    visit.setPropertyAddress(event.getPropertyAddress());
    visit.setAgentId(event.getAgentId());
    visit.setAgentName(event.getAgentName());
    visit.setVehicleId(event.getVehicleId());
    visit.setTravelTimeGo(event.getTravelTimeGo());
    visit.setTravelTimeBack(event.getTravelTimeBack());
    visit.setStartTime(event.getStartTime());
    visit.setEndTime(event.getEndTime());
    visit.setType(Visit.EventType.valueOf(event.getType().name()));
    visit.setStatus(Visit.EventStatus.valueOf(event.getStatus().name()));
    visit.setNotes(event.getNotes());
    visit.setCreatedAt(event.getCreatedAt());
    visit.setClientId(event.getClientId());
    visit.setClientName(event.getClientName());

    if (event.getResultado() != null) {
      try {
        visit.setResultado(Visit.ResultadoVisita.valueOf(event.getResultado()));
      } catch (IllegalArgumentException e) {
        // Ignorar si el resultado no coincide con el enum
      }
    }
    visit.setObservaciones(event.getObservaciones());
    visit.setFechaRegistroResultado(event.getFechaRegistroResultado());

    // PA1 de HU1: marca visualmente los eventos del agente autenticado
    visit.setOwnEvent(requestingAgentId != null && requestingAgentId.equals(event.getAgentId()));
    // Inicializar históricos vacíos
    visit.setReassignmentHistory(new ArrayList<>());
    visit.setReschedulingHistory(new ArrayList<>());
    visit.setOriginVisitId(null);
    visitRepository.save(visit);
    return visit;
  }
}
