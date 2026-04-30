package com.inmobiliaria.visit_calendar_service.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.inmobiliaria.visit_calendar_service.dto.SendNotificationRequest;
import com.inmobiliaria.visit_calendar_service.dto.VisitCalendarDTOs.CreateVisitRequest;
import com.inmobiliaria.visit_calendar_service.model.VisitRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

  private final RestTemplate restTemplate; // será inyectado por el constructor

  @Value("${notification.service.url:http://localhost:8083}")
  private String notificationServiceUrl;

  // Notifica al agente por solicitud de visita (ya existente)
  public boolean notifyAgentOfVisitRequest(VisitRequest visitRequest) {
    try {
      String endpoint = notificationServiceUrl + "/notifications/visit-request";
      Map<String, Object> payload =
          Map.of(
              "type", "VISIT_REQUEST",
              "recipientId", visitRequest.getAgentId(),
              "subject", "Nueva solicitud de visita - " + visitRequest.getPropertyName(),
              "message", buildNotificationMessage(visitRequest),
              "metadata",
                  Map.of(
                      "visitRequestId", visitRequest.getId(),
                      "propertyId", visitRequest.getPropertyId(),
                      "propertyName", visitRequest.getPropertyName(),
                      "clientName", visitRequest.getClientName(),
                      "clientEmail", visitRequest.getClientEmail(),
                      "preferredDate", visitRequest.getPreferredDateTime().toString()));
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);
      if (response.getStatusCode().is2xxSuccessful()) {
        log.info(
            "Notificación enviada al agente {} para la propiedad {}",
            visitRequest.getAgentId(),
            visitRequest.getPropertyName());
        return true;
      }
    } catch (Exception e) {
      log.warn(
          "No se pudo conectar con el notification-service. La solicitud se guardó igualmente. Error: {}",
          e.getMessage());
    }
    log.info(
        "[NOTIFICACIÓN INTERNA] Agente '{}' — Nueva solicitud de visita de '{}' para el inmueble '{}' el {}",
        visitRequest.getAgentName(),
        visitRequest.getClientName(),
        visitRequest.getPropertyName(),
        visitRequest.getPreferredDateTime());
    return false;
  }

  // Notifica al propietario del inmueble cuando se programa una visita
  public void notifyPropertyOwner(String ownerEmail, String ownerName, CreateVisitRequest visit) {
    SendNotificationRequest req =
        new SendNotificationRequest(
            ownerEmail,
            "VISIT_SCHEDULED",
            "Nueva visita programada - " + visit.getPropertyName(),
            buildPropertyOwnerMessage(ownerName, visit),
            "EMAIL");
    restTemplate.postForEntity(notificationServiceUrl + "/notifications/send", req, Void.class);
  }

  private String buildPropertyOwnerMessage(String ownerName, CreateVisitRequest visit) {
    return String.format(
        "Estimado/a %s,\n\nSe ha programado una visita a su propiedad '%s'.\n\n"
            + "Detalles:\n• Dirección: %s\n• Fecha y hora: %s - %s\n• Agente a cargo: %s (%s)\n• Notas: %s\n\n"
            + "Puede consultar el historial de visitas en el sistema.",
        ownerName,
        visit.getPropertyName(),
        visit.getPropertyAddress() != null ? visit.getPropertyAddress() : "No especificada",
        visit.getStartTime(),
        visit.getEndTime(),
        visit.getAgentName(),
        visit.getAgentId(),
        visit.getNotes() != null ? visit.getNotes() : "Sin observaciones");
  }

  private String buildNotificationMessage(VisitRequest r) {
    return String.format(
        "El cliente %s (%s) ha solicitado una visita al inmueble '%s'.\n"
            + "Horario preferido: %s\n"
            + "%s"
            + "Mensaje del cliente: %s\n\n"
            + "Por favor ingresa al sistema para aceptar o rechazar la solicitud.",
        r.getClientName(),
        r.getClientEmail(),
        r.getPropertyName(),
        r.getPreferredDateTime(),
        r.getAlternativeDateTime() != null
            ? "Horario alternativo: " + r.getAlternativeDateTime() + "\n"
            : "",
        r.getMessage() != null ? r.getMessage() : "(Sin mensaje)");
  }
}
