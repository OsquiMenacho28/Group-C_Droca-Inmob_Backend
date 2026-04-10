package com.inmobiliaria.visit_calendar_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.inmobiliaria.visit_calendar_service.dto.ReassignmentRequestRequestDTO;
import com.inmobiliaria.visit_calendar_service.dto.ReassignmentRequestResponseDTO;
import com.inmobiliaria.visit_calendar_service.dto.RequestResponseDTO;
import com.inmobiliaria.visit_calendar_service.model.CalendarEvent;
import com.inmobiliaria.visit_calendar_service.model.ReassignmentHistory;
import com.inmobiliaria.visit_calendar_service.model.ReassignmentRequest;
import com.inmobiliaria.visit_calendar_service.repository.CalendarEventRepository;
import com.inmobiliaria.visit_calendar_service.repository.ReassignmentRequestRepository;
import com.inmobiliaria.visit_calendar_service.repository.VisitRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Servicio que contiene toda la lógica de negocio para la reasignación de
 * citas.
 *
 * Flujo principal:
 * 1. Agente original solicita reasignación → se crea SolicitudReasignacion en
 * estado PENDIENTE.
 * 2. Agente destino acepta o rechaza → se actualiza el estado de la solicitud.
 * - Si ACEPTADA: se actualiza CalendarEvent.agentId al nuevo agente y se guarda el
 * historial.
 * - Si RECHAZADA: la cita permanece sin cambios.
 * 3. Se dispara notificación al agente solicitante con la decisión.
 */
@Slf4j
@Service
public class ReassignmentService {

    private final ReassignmentRequestRepository reassignmentRequestRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final VisitRepository visitRepository;
    private final ReassignmentNotificationService reassignmentNotificationService;
    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${person.service.url:http://localhost:8084}")
    private String personServiceUrl;

    public ReassignmentService(ReassignmentRequestRepository reassignmentRequestRepository,
                               CalendarEventRepository calendarEventRepository,
                               VisitRepository visitRepository,
                               ReassignmentNotificationService reassignmentNotificationService) {
        this.reassignmentRequestRepository = reassignmentRequestRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.visitRepository = visitRepository;
        this.reassignmentNotificationService = reassignmentNotificationService;
        this.restTemplate = new RestTemplate();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREAR SOLICITUD DE REASIGNACIÓN
    // POST /api/visits/{visitId}/reassignment
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crea una nueva solicitud de reasignación.
     *
     * @param visitId           ID de la cita a reasignar
     * @param requestingAgentId ID del agente que realiza la solicitud (del JWT)
     * @param dto               Datos de la solicitud (agenteDestino + motivo)
     * @return DTO con la solicitud creada
     */
    public ReassignmentRequestResponseDTO requestReassignment(String visitId,
            String requestingAgentId,
            ReassignmentRequestRequestDTO dto) {
        // 1. Buscar en calendar_events (NO en visits)
        CalendarEvent event = calendarEventRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + visitId));

        if (!event.getAgentId().equals(requestingAgentId)) {
            throw new RuntimeException("No tienes permiso para reasignar esta cita: no te pertenece.");
        }

        // 2. Verificar que no hay una solicitud PENDIENTE ya existente para esta cita
        List<ReassignmentRequest> pending = reassignmentRequestRepository.findByVisitIdAndStatus(
                visitId, ReassignmentRequest.RequestStatus.PENDING);
        if (!pending.isEmpty()) {
            throw new RuntimeException("Ya existe una solicitud de reasignación pendiente para esta cita.");
        }

        // 3. Verificar que el agente destino no es el mismo que el solicitante
        if (requestingAgentId.equals(dto.getDestinationAgentId())) {
            throw new RuntimeException("No puedes reasignar la cita a ti mismo.");
        }

        // 4. Crear y guardar la solicitud
        ReassignmentRequest request = new ReassignmentRequest(
                visitId,
                requestingAgentId,
                dto.getDestinationAgentId(),
                dto.getReason());
        request = reassignmentRequestRepository.save(request);

        // 5. Notificar al agente destino sobre la solicitud recibida
        reassignmentNotificationService.notifyReassignmentRequest(request, event);

        return ReassignmentRequestResponseDTO.from(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESPONDER SOLICITUD (ACEPTAR / RECHAZAR)
    // PUT /api/reassignments/{requestId}/reply
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Procesa la respuesta (aceptar/rechazar) de un agente destino.
     *
     * @param requestId          ID de la solicitud
     * @param destinationAgentId ID del agente que responde (del JWT)
     * @param dto                Decisión + comentario opcional
     * @return DTO con la solicitud actualizada
     */
    public ReassignmentRequestResponseDTO replyRequest(String requestId,
            String destinationAgentId,
            RequestResponseDTO dto) {
        // 1. Recuperar la solicitud
        ReassignmentRequest request = reassignmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + requestId));

        // 2. Verificar que el que responde es el agente destino
        if (!request.getDestinationAgentId().equals(destinationAgentId)) {
            throw new RuntimeException("No tienes permiso para responder esta solicitud.");
        }

        // 3. Verificar que la solicitud sigue PENDIENTE
        if (request.getStatus() != ReassignmentRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Esta solicitud ya fue respondida.");
        }

        // 4. Validar que la decisión es ACEPTADA o RECHAZADA
        if (dto.getDecision() == ReassignmentRequest.RequestStatus.PENDING) {
            throw new RuntimeException("La decisión debe ser ACEPTADA o RECHAZADA.");
        }

        // 5. Actualizar la solicitud
        request.setStatus(dto.getDecision());
        request.setRepliedAt(LocalDateTime.now());
        request.setCommentReply(dto.getComment());

        // 6. Si ACEPTADA → reasignar la cita
        if (dto.getDecision() == ReassignmentRequest.RequestStatus.ACCEPTED) {
            applyReassignment(request);
        }

        request = reassignmentRequestRepository.save(request);

        // 7. Notificar al agente solicitante con la decisión
        reassignmentNotificationService.notifyReassignmentDecision(request);

        return ReassignmentRequestResponseDTO.from(request);
    }

    /**
     * Aplica la reasignación en CalendarEvent:
     * - Cambia agentId al agente destino
     * - Actualiza agentName con el nombre completo del nuevo agente
     */
    private void applyReassignment(ReassignmentRequest request) {
        CalendarEvent event = calendarEventRepository.findById(request.getVisitId())
                .orElseThrow(() -> new RuntimeException("Cita no encontrada al aplicar reasignación."));

        // Obtener el nombre completo del agente destino
        String destinationAgentName = getAgentFullName(request.getDestinationAgentId());
        
        // Actualizar el evento en el calendario
        event.setAgentId(request.getDestinationAgentId());
        event.setAgentName(destinationAgentName);
        
        calendarEventRepository.save(event);
        log.info("Reasignación aplicada: evento '{}' (propiedad '{}') ahora asignado a agente '{}' ('{}')", 
                 event.getId(), event.getPropertyName(), request.getDestinationAgentId(), destinationAgentName);
        
        // También actualizar la entidad Visit si existe para mantener consistencia y el historial
        visitRepository.findById(request.getVisitId()).ifPresent(visit -> {
            String previousAgent = visit.getAgentId();
            
            ReassignmentHistory reassignmentHistory = new ReassignmentHistory(
                    request.getId(),
                    previousAgent,
                    request.getDestinationAgentId(),
                    request.getReason());
    
            if (visit.getReassignmentHistory() == null) {
                visit.setReassignmentHistory(new java.util.ArrayList<>());
            }
            visit.getReassignmentHistory().add(reassignmentHistory);
            visit.setAgentId(request.getDestinationAgentId());
            visitRepository.save(visit);
            log.debug("Historial de reasignación actualizado en la entidad Visit: {}", visit.getId());
        });
    }

    /**
     * Obtiene el nombre completo del agente consultando el microservicio de usuarios.
     * Intenta primero con user-service, luego con person-service como fallback.
     *
     * @param agentId ID del agente (authUserId)
     * @return nombre completo (firstName + lastName) o el ID si falla la consulta
     */
    private String getAgentFullName(String agentId) {
        // Primero intentar con user-service
        try {
            String url = userServiceUrl + "/users/" + agentId;
            log.debug("Consultando user-service para obtener nombre del agente: {}", url);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                String firstName = (String) response.get("firstName");
                String lastName = (String) response.get("lastName");
                
                if (firstName != null && lastName != null) {
                    return firstName + " " + lastName;
                } else if (firstName != null) {
                    return firstName;
                } else if (lastName != null) {
                    return lastName;
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener el nombre del agente desde user-service para ID: {}. Intentando con person-service...", agentId, e);
        }
        
        // Fallback: intentar con person-service
        try {
            String url = personServiceUrl + "/persons/by-auth/" + agentId;
            log.debug("Consultando person-service como fallback: {}", url);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                String firstName = (String) response.get("firstName");
                String lastName = (String) response.get("lastName");
                
                if (firstName != null && lastName != null) {
                    return firstName + " " + lastName;
                } else if (firstName != null) {
                    return firstName;
                } else if (lastName != null) {
                    return lastName;
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener el nombre del agente desde person-service para ID: {}", agentId, e);
        }
        
        // Fallback final: devolver el ID como nombre
        log.warn("Usando ID como fallback para el nombre del agente: {}", agentId);
        return agentId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSULTAR SOLICITUDES RECIBIDAS PENDIENTES
    // GET /api/reassignments/received
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retorna todas las solicitudes PENDIENTES recibidas por el agente autenticado.
     *
     * @param destinationAgentId ID del agente destino (del JWT)
     * @return Lista de solicitudes pendientes
     */
    public List<ReassignmentRequestResponseDTO> getReceivedRequests(String destinationAgentId) {
        return reassignmentRequestRepository.findByDestinationAgentIdAndStatus(
                destinationAgentId,
                ReassignmentRequest.RequestStatus.PENDING)
                .stream()
                .map(ReassignmentRequestResponseDTO::from)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONTAR SOLICITUDES PENDIENTES (para badge de menú)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retorna el número de solicitudes pendientes para el agente.
     */
    public long countPendingRequests(String destinationAgentId) {
        return reassignmentRequestRepository.countByDestinationAgentIdAndStatus(
                destinationAgentId,
                ReassignmentRequest.RequestStatus.PENDING);
    }
}