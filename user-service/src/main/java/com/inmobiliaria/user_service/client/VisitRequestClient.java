package com.inmobiliaria.user_service.client;

import com.inmobiliaria.user_service.config.FeignConfig;
import java.time.Instant;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "visit-calendar-service", configuration = FeignConfig.class)
public interface VisitRequestClient {

  @GetMapping("/visit-requests/client/{clientId}")
  List<VisitRequestSummary> getClientRequests(@PathVariable("clientId") String clientId);

  record VisitRequestSummary(
      String id,
      String propertyId,
      String propertyName,
      String agentId,
      String agentName,
      String clientId,
      String message,
      String status,
      Instant createdAt) {}
}
