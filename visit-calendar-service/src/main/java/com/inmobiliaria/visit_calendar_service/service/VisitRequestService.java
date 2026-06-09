package com.inmobiliaria.visit_calendar_service.service;

import com.inmobiliaria.visit_calendar_service.client.ClientInteractionClient;
import com.inmobiliaria.visit_calendar_service.dto.RegistrarResultadoRequest;
import com.inmobiliaria.visit_calendar_service.dto.VisitCalendarDTOs.*;
import com.inmobiliaria.visit_calendar_service.exception.ResourceNotFoundException;
import com.inmobiliaria.visit_calendar_service.model.CalendarEvent;
import com.inmobiliaria.visit_calendar_service.model.Visit;
import com.inmobiliaria.visit_calendar_service.model.Visit.ResultadoVisita;
import com.inmobiliaria.visit_calendar_service.model.VisitRequest;
import com.inmobiliaria.visit_calendar_service.repository.CalendarEventRepository;
import com.inmobiliaria.visit_calendar_service.repository.VisitRepository;
import com.inmobiliaria.visit_calendar_service.repository.VisitRequestRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de lógica de negocio para solicitudes de visita de clientes.
 *
 * <p>HU3: Cliente buscador solicita agendar una cita. - PA1: El cliente solo ve propiedades
 * disponibles (lógica en property-service, aquí solo el POST). - PA2: Al crear la solicitud, se
 * genera una notificación al agente responsable. - PA3: Filtros de búsqueda (manejados en
 * property-service, aquí la cita).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitRequestService {

  private final VisitRequestRepository visitRequestRepository;
  private final CalendarEventRepository calendarEventRepository;
  private final NotificationService notificationService;
  private final VisitRepository visitRepository;
  private final VehicleUsageService vehicleUsageService;
  private final ClientInteractionClient clientInteractionClient;

  /**
   * PA1 + PA2 de HU3: El cliente solicita una cita para un inmueble. Se persiste la solicitud y se
   * envía notificación al agente.
   */
  public VisitRequestResponse createVisitRequest(ClientVisitRequestDTO dto) {
    log.debug(
        "Nueva solicitud de visita: cliente={}, propiedad={}",
        dto.getClientId(),
        dto.getPropertyId());

    VisitRequest request =
        VisitRequest.builder()
            .propertyId(dto.getPropertyId())
            .propertyName(dto.getPropertyName())
            .agentId(dto.getAgentId())
            .agentName(dto.getAgentName())
            .clientId(dto.getClientId())
            .clientName(dto.getClientName())
            .clientEmail(dto.getClientEmail())
            .clientPhone(dto.getClientPhone())
            .preferredDateTime(dto.getPreferredDateTime())
            .alternativeDateTime(dto.getAlternativeDateTime())
            .message(dto.getMessage())
            .status(VisitRequest.RequestStatus.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .notificationSent(false)
            .build();

    VisitRequest saved = visitRequestRepository.save(request);

    // PA2: Notificar al agente responsable del inmueble
    boolean notified = notificationService.notifyAgentOfVisitRequest(saved);
    if (notified) {
      saved.setNotificationSent(true);
      saved = visitRequestRepository.save(saved);
      log.info(
          "Notificación enviada al agente {}: solicitud de visita id={}",
          saved.getAgentId(),
          saved.getId());
    } else {
      log.warn(
          "No se pudo enviar la notificación al agente {} para la solicitud {}",
          saved.getAgentId(),
          saved.getId());
    }

    recordClientInteraction(saved);

    return toResponse(saved);
  }

  private void recordClientInteraction(VisitRequest request) {
    try {
      clientInteractionClient.recordInteraction(
          new ClientInteractionClient.RecordClientInteractionRequest(
              request.getClientId(),
              request.getAgentId(),
              request.getPropertyId(),
              request.getPropertyName(),
              request.getAgentName(),
              "VISITA",
              request.getCreatedAt(),
              request.getMessage(),
              request.getStatus() != null ? request.getStatus().name() : null,
              "visit-request-" + request.getId()));
    } catch (Exception e) {
      log.warn(
          "Failed to record client interaction for visit request {}: {}",
          request.getId(),
          e.getMessage());
    }
  }

  /** El agente acepta la solicitud de visita y crea el evento en el calendario. */
  public VisitRequestResponse acceptVisitRequest(String requestId, String agentId) {
    VisitRequest request =
        visitRequestRepository
            .findById(requestId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Solicitud no encontrada: " + requestId));

    if (!request.getAgentId().equals(agentId)) {
      throw new IllegalArgumentException("Solo el agente responsable puede aceptar esta solicitud");
    }

    // Crear evento en el calendario
    CalendarEvent event =
        CalendarEvent.builder()
            .propertyId(request.getPropertyId())
            .propertyName(request.getPropertyName())
            .agentId(request.getAgentId())
            .agentName(request.getAgentName())
            .clientId(request.getClientId())
            .clientName(request.getClientName())
            .startTime(request.getPreferredDateTime())
            .endTime(request.getPreferredDateTime().plus(1, java.time.temporal.ChronoUnit.HOURS))
            .type(CalendarEvent.EventType.CLIENT_REQUEST)
            .status(CalendarEvent.EventStatus.CONFIRMED)
            .notes("Visita solicitada por el cliente: " + request.getClientName())
            .createdAt(Instant.now())
            .build();

    CalendarEvent savedEvent = calendarEventRepository.save(event);

    // Actualizar solicitud
    request.setStatus(VisitRequest.RequestStatus.ACCEPTED);
    request.setCalendarEventId(savedEvent.getId());
    request.setUpdatedAt(Instant.now());
    VisitRequest updated = visitRequestRepository.save(request);

    log.info("Solicitud aceptada: requestId={}, calendarEventId={}", requestId, savedEvent.getId());
    return toResponse(updated);
  }

  /** El agente rechaza la solicitud de visita. */
  public VisitRequestResponse rejectVisitRequest(String requestId, String agentId) {
    VisitRequest request =
        visitRequestRepository
            .findById(requestId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Solicitud no encontrada: " + requestId));

    if (!request.getAgentId().equals(agentId)) {
      throw new IllegalArgumentException(
          "Solo el agente responsable puede rechazar esta solicitud");
    }

    request.setStatus(VisitRequest.RequestStatus.REJECTED);
    request.setUpdatedAt(Instant.now());
    VisitRequest updated = visitRequestRepository.save(request);

    log.info("Solicitud rechazada: requestId={}", requestId);
    return toResponse(updated);
  }

  /** Obtiene todas las solicitudes pendientes de un agente. */
  public List<VisitRequestResponse> getPendingRequestsForAgent(String agentId) {
    return visitRequestRepository
        .findByAgentIdAndStatus(agentId, VisitRequest.RequestStatus.PENDING)
        .stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  /** Obtiene todas las solicitudes de un cliente. */
  public List<VisitRequestResponse> getClientRequests(String clientId) {
    return visitRequestRepository.findByClientId(clientId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  public int getVisitCountForProperty(String propertyId) {
    return calendarEventRepository.findByPropertyId(propertyId).size();
    // return visitRequestRepository.findByPropertyId(propertyId).size();
  }

  @Transactional
  public Visit registrarResultado(String id, RegistrarResultadoRequest request, String agentId) {
    CalendarEvent event =
        calendarEventRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Visita no encontrada en calendario"));

    // Permitir registro en visitas PROGRAMADAS o CONFIRMADAS
    if (event.getStatus() != CalendarEvent.EventStatus.SCHEDULED
        && event.getStatus() != CalendarEvent.EventStatus.CONFIRMED) {
      throw new IllegalStateException(
          "Solo se puede registrar resultado en visitas con estado PROGRAMADA o CONFIRMADA. Estado actual: "
              + event.getStatus());
    }
    // validar que el agente autenticado sea el asignado a la visita (seguridad)
    if (!event.getAgentId().equals(agentId)) {
      throw new SecurityException("No tienes permiso para modificar esta visita");
    }

    event.setResultado(request.resultado());
    event.setObservaciones(request.observaciones());
    event.setFechaRegistroResultado(Instant.now());
    event.setStatus(CalendarEvent.EventStatus.REALIZADA);

    CalendarEvent savedEvent = calendarEventRepository.save(event);

    Visit visitToReturn = new Visit();

    // Sincronizar en VisitRepository si existe
    visitRepository
        .findById(id)
        .ifPresentOrElse(
            visit -> {
              visit.setResultado(ResultadoVisita.valueOf(request.resultado()));
              visit.setObservaciones(request.observaciones());
              visit.setFechaRegistroResultado(savedEvent.getFechaRegistroResultado());
              visit.setStatus(Visit.EventStatus.REALIZADA);
              visitRepository.save(visit);

              // Registrar uso del vehículo si aplica, usando la visit sincronizada
              vehicleUsageService.recordUsage(visit, request.mileage());
            },
            () -> {
              // Si no existe (porque es nueva de CalendarEvent), crear mock para
              // vehicleUsageService
              Visit mockVisit = new Visit();
              mockVisit.setId(savedEvent.getId());
              mockVisit.setVehicleId(savedEvent.getVehicleId());
              mockVisit.setStartTime(savedEvent.getStartTime());
              mockVisit.setEndTime(savedEvent.getEndTime());
              mockVisit.setTravelTimeGo(savedEvent.getTravelTimeGo());
              mockVisit.setTravelTimeBack(savedEvent.getTravelTimeBack());
              mockVisit.setStatus(Visit.EventStatus.REALIZADA);
              mockVisit.setResultado(ResultadoVisita.valueOf(request.resultado()));

              // Mapear info para retorno
              visitToReturn.setId(savedEvent.getId());
              visitToReturn.setPropertyId(savedEvent.getPropertyId());
              visitToReturn.setPropertyName(savedEvent.getPropertyName());
              visitToReturn.setAgentId(savedEvent.getAgentId());
              visitToReturn.setAgentName(savedEvent.getAgentName());
              visitToReturn.setStartTime(savedEvent.getStartTime());
              visitToReturn.setEndTime(savedEvent.getEndTime());
              visitToReturn.setStatus(mockVisit.getStatus());
              visitToReturn.setResultado(mockVisit.getResultado());
              visitToReturn.setObservaciones(savedEvent.getObservaciones());
              visitToReturn.setFechaRegistroResultado(savedEvent.getFechaRegistroResultado());

              vehicleUsageService.recordUsage(mockVisit, request.mileage());
            });

    if (visitToReturn.getId() == null) { // if the ifPresent executed, visitToReturn is still empty
      visitToReturn.setId(savedEvent.getId());
      visitToReturn.setPropertyId(savedEvent.getPropertyId());
      visitToReturn.setPropertyName(savedEvent.getPropertyName());
      visitToReturn.setAgentId(savedEvent.getAgentId());
      visitToReturn.setAgentName(savedEvent.getAgentName());
      visitToReturn.setStartTime(savedEvent.getStartTime());
      visitToReturn.setEndTime(savedEvent.getEndTime());
      visitToReturn.setStatus(Visit.EventStatus.REALIZADA);
      visitToReturn.setResultado(ResultadoVisita.valueOf(savedEvent.getResultado()));
      visitToReturn.setObservaciones(savedEvent.getObservaciones());
      visitToReturn.setFechaRegistroResultado(savedEvent.getFechaRegistroResultado());
    }

    return visitToReturn;
  }

  private VisitRequestResponse toResponse(VisitRequest r) {
    return VisitRequestResponse.builder()
        .id(r.getId())
        .propertyId(r.getPropertyId())
        .propertyName(r.getPropertyName())
        .agentId(r.getAgentId())
        .agentName(r.getAgentName())
        .clientId(r.getClientId())
        .clientName(r.getClientName())
        .clientEmail(r.getClientEmail())
        .clientPhone(r.getClientPhone())
        .preferredDateTime(r.getPreferredDateTime())
        .alternativeDateTime(r.getAlternativeDateTime())
        .message(r.getMessage())
        .status(r.getStatus())
        .calendarEventId(r.getCalendarEventId())
        .createdAt(r.getCreatedAt())
        .notificationSent(r.isNotificationSent())
        .build();
  }
}
