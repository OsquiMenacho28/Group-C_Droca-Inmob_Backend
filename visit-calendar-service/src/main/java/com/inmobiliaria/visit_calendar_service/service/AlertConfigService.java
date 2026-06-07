// backend/visit-calendar-service/src/main/java/.../service/AlertConfigService.java
package com.inmobiliaria.visit_calendar_service.service;

import com.inmobiliaria.visit_calendar_service.model.AlertConfig;
import com.inmobiliaria.visit_calendar_service.repository.AlertConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertConfigService {
  private final AlertConfigRepository repository;

  private static final String CONFIG_ID = "DEFAULT";

  public AlertConfig getConfig() {
    return repository
        .findById(CONFIG_ID)
        .orElseGet(
            () -> {
              AlertConfig defaultConfig = new AlertConfig(CONFIG_ID, 2, "IN_APP", true);
              return repository.save(defaultConfig);
            });
  }

  public AlertConfig updateConfig(int anticipationHours, String channel) {
    AlertConfig config = getConfig();
    config.setAnticipationHours(anticipationHours);
    config.setChannel(channel);
    return repository.save(config);
  }
}
