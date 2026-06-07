// backend/visit-calendar-service/src/main/java/.../service/UpcomingVisitsNotifier.java
package com.inmobiliaria.visit_calendar_service.service;

import com.inmobiliaria.visit_calendar_service.client.NotificationClient;
import com.inmobiliaria.visit_calendar_service.domain.InteractionType;
import com.inmobiliaria.visit_calendar_service.dto.request.SendInAppNotificationRequest;
import com.inmobiliaria.visit_calendar_service.model.AlertConfig;
import com.inmobiliaria.visit_calendar_service.model.Visit;
import com.inmobiliaria.visit_calendar_service.repository.VisitRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpcomingVisitsNotifier {

  private final VisitRepository visitRepository;
  private final AlertConfigService alertConfigService;
  private final NotificationClient notificationClient;

  @Scheduled(fixedDelay = 15 * 60 * 1000) // cada 15 minutos
  @Transactional
  public void processUpcomingVisits() {
    AlertConfig config = alertConfigService.getConfig();
    if (!config.isEnabled()) {
      log.debug("Alertas deshabilitadas, scheduler salta.");
      return;
    }

    Instant now = Instant.now();
    Instant limit = now.plus(config.getAnticipationHours(), ChronoUnit.HOURS);

    List<Visit> upcomingVisits =
        visitRepository.findByStartTimeBetweenAndStatus(now, limit, Visit.EventStatus.SCHEDULED);

    for (Visit visit : upcomingVisits) {
      if (visit.isUpcomingNotificationSent()) {
        continue;
      }

      // Enviar notificación al agente responsable
      sendNotificationToUser(visit.getAgentId(), visit, "agente");
      // Opcional: también al propietario (requiere obtener ownerId)
      // sendNotificationToOwner(visit.getPropertyId(), visit);

      visit.setUpcomingNotificationSent(true);
      visitRepository.save(visit);
    }
  }

  private void sendNotificationToUser(String userId, Visit visit, String role) {
    try {
      String subject = "📅 Visita próxima";
      String content =
          String.format(
              "Tienes una visita programada para la propiedad '%s' el día %s.",
              visit.getPropertyName(), visit.getStartTime());

      SendInAppNotificationRequest request =
          new SendInAppNotificationRequest(
              userId,
              "UPCOMING_VISIT",
              InteractionType.VISITA,
              List.of(),
              subject,
              content,
              Map.of("visitId", visit.getId(), "propertyId", visit.getPropertyId()));

      notificationClient.sendInAppNotification(request);
      log.info("Notificación de visita próxima enviada a {} {}", role, userId);
    } catch (Exception e) {
      log.error("Error enviando notificación de visita próxima a {}: {}", userId, e.getMessage());
    }
  }
}
