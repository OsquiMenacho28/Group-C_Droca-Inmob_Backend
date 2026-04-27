package com.inmobiliaria.property_service.client;

import java.time.LocalDateTime;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import lombok.Builder;

@FeignClient(name = "operation-service")
public interface OperationClient {

  @PostMapping("/operations")
  void createOperation(
      @RequestBody CreateOperationRequest request,
      @RequestHeader("X-Auth-User-Id") String userId,
      @RequestHeader("X-Auth-Roles") String roles);

  @Builder
  record CreateOperationRequest(
      String propertyId,
      String propertyName,
      String propertyType,
      String operationType,
      Double finalPrice,
      String currency,
      String clientId,
      String clientName,
      String agentId,
      String agentName,
      String ownerId,
      String ownerName,
      String department,
      String status,
      String notes,
      LocalDateTime closureDate) {}
}
