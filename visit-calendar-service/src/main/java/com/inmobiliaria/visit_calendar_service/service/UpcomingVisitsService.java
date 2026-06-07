// backend/visit-calendar-service/src/main/java/.../service/UpcomingVisitsService.java
package com.inmobiliaria.visit_calendar_service.service;

import com.inmobiliaria.visit_calendar_service.model.AlertConfig;
import com.inmobiliaria.visit_calendar_service.model.Visit;
import com.inmobiliaria.visit_calendar_service.repository.VisitRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpcomingVisitsService {

  private final VisitRepository visitRepository;
  private final AlertConfigService alertConfigService;

  public List<Visit> getUpcomingVisitsForUser(String userId, Integer customHours) {
    AlertConfig config = alertConfigService.getConfig();
    int hours =
        (customHours != null && customHours > 0) ? customHours : config.getAnticipationHours();

    Instant now = Instant.now();
    Instant limit = now.plus(hours, ChronoUnit.HOURS);

    // Visitas donde el usuario es agente o propietario (se debe mapear ownerId desde
    // property-service)
    // Simplificación: solo por agentId (extensible)
    return visitRepository.findByAgentIdAndStartTimeBetweenAndStatus(
        userId, now, limit, Visit.EventStatus.SCHEDULED);
  }
}
