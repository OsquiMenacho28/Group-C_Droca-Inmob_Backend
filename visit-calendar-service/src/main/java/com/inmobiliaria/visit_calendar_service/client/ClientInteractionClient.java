package com.inmobiliaria.visit_calendar_service.client;

import com.inmobiliaria.visit_calendar_service.config.FeignConfig;
import java.time.Instant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "user-service",
    contextId = "clientInteractionClient",
    configuration = FeignConfig.class)
public interface ClientInteractionClient {

  @PostMapping("/clients/interactions")
  void recordInteraction(@RequestBody RecordClientInteractionRequest request);

  record RecordClientInteractionRequest(
      String clientId,
      String agentId,
      String propertyId,
      String propertyName,
      String agentName,
      String type,
      Instant occurredAt,
      String detail,
      String subType,
      String referenceId) {}
}
