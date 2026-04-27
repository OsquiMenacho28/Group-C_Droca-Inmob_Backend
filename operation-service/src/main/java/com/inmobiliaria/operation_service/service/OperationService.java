package com.inmobiliaria.operation_service.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.inmobiliaria.operation_service.client.PropertyClient;
import com.inmobiliaria.operation_service.domain.OperationDocument;
import com.inmobiliaria.operation_service.dto.OperationRequest;
import com.inmobiliaria.operation_service.dto.OperationResponse;
import com.inmobiliaria.operation_service.exception.ResourceNotFoundException;
import com.inmobiliaria.operation_service.repository.OperationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationService {

  private final OperationRepository operationRepository;
  private final PropertyClient propertyClient;

  public List<OperationResponse> findAll() {
    return operationRepository.findAll().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public OperationResponse findById(String id) {
    OperationDocument operation =
        operationRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Operation not found with id: " + id));
    return mapToResponse(operation);
  }

  public OperationResponse create(OperationRequest request, String userId, String roles) {
    // Prevent duplicate closure
    if ("CLOSED".equalsIgnoreCase(request.getStatus())) {
      operationRepository
          .findByPropertyIdAndStatus(request.getPropertyId(), "CLOSED")
          .ifPresent(
              op -> {
                throw new com.inmobiliaria.operation_service.exception.ValidationException(
                    "This property already has a CLOSED operation record.");
              });
    }

    OperationDocument operation =
        OperationDocument.builder()
            .propertyId(request.getPropertyId())
            .propertyName(request.getPropertyName())
            .propertyType(request.getPropertyType())
            .operationType(request.getOperationType())
            .finalPrice(request.getFinalPrice())
            .currency(request.getCurrency())
            .clientId(request.getClientId())
            .clientName(request.getClientName())
            .agentId(request.getAgentId())
            .agentName(request.getAgentName())
            .ownerId(request.getOwnerId())
            .ownerName(request.getOwnerName())
            .department(request.getDepartment())
            .status(request.getStatus())
            .notes(request.getNotes())
            .closureDate(
                request.getClosureDate() != null ? request.getClosureDate() : LocalDateTime.now())
            .build();

    operation.setCreatedAt(Instant.now());
    operation.setCreatedBy(userId);

    OperationDocument saved = operationRepository.save(operation);

    try {
      syncPropertyStatus(saved.getPropertyId(), saved.getStatus(), userId);
    } catch (Exception e) {
      log.error("Failed to sync status, rolling back operation creation: {}", e.getMessage());
      operationRepository.delete(saved);
      throw new com.inmobiliaria.operation_service.exception.ValidationException(
          "Failed to update property status. Operation cancelled: " + e.getMessage());
    }

    return mapToResponse(saved);
  }

  public OperationResponse updateStatus(String id, String status, String userId, String roles) {
    OperationDocument operation =
        operationRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Operation not found with id: " + id));

    String oldStatus = operation.getStatus();

    // Bloquear cualquier cambio si la operación ya está CERRADA, excepto para CANCELARLA si es
    // inválida
    if ("CLOSED".equalsIgnoreCase(oldStatus) && !"CANCELLED".equalsIgnoreCase(status)) {
      throw new com.inmobiliaria.operation_service.exception.ValidationException(
          "Esta operación ya ha sido CERRADA de forma definitiva y no puede ser modificada.");
    }

    // Si ya está CANCELADA, no se puede mover a otro estado
    if ("CANCELLED".equalsIgnoreCase(oldStatus)) {
      throw new com.inmobiliaria.operation_service.exception.ValidationException(
          "La operación ya ha sido cancelada y no puede ser reactivada.");
    }

    operation.setStatus(status);
    operation.setUpdatedAt(Instant.now());

    OperationDocument saved = operationRepository.save(operation);

    try {
      syncPropertyStatus(saved.getPropertyId(), status, userId);
    } catch (Exception e) {
      log.error("Failed to sync status, rolling back status update: {}", e.getMessage());
      operation.setStatus(oldStatus);
      operationRepository.save(operation);
      throw new com.inmobiliaria.operation_service.exception.ValidationException(
          "Failed to update property status. Reverting operation status: " + e.getMessage());
    }

    return mapToResponse(saved);
  }

  private void syncPropertyStatus(String propertyId, String operationStatus, String userId) {
    String propertyStatus = mapOperationToPropertyStatus(operationStatus);
    if (propertyStatus != null) {
      propertyClient.updateStatus(
          propertyId, new PropertyClient.UpdateStatusRequest(propertyStatus), userId, true);
      log.info(
          "Synced property {} status to {} due to operation status {}",
          propertyId,
          propertyStatus,
          operationStatus);
    }
  }

  private String mapOperationToPropertyStatus(String operationStatus) {
    return switch (operationStatus.toUpperCase()) {
      case "CLOSED" -> "VENDIDO";
      case "PENDING" -> "RESERVADO";
      case "ACTIVE" -> "EN_NEGOCIACION";
      // Cuando se cancela una operación, NO se revierte el estado de la propiedad para asegurar
      // consistencia
      case "CANCELLED" -> null;
      default -> null;
    };
  }

  private OperationResponse mapToResponse(OperationDocument op) {
    return OperationResponse.builder()
        .id(op.getId())
        .propertyId(op.getPropertyId())
        .propertyName(op.getPropertyName())
        .propertyType(op.getPropertyType())
        .operationType(op.getOperationType())
        .finalPrice(op.getFinalPrice())
        .currency(op.getCurrency())
        .clientId(op.getClientId())
        .clientName(op.getClientName())
        .agentId(op.getAgentId())
        .agentName(op.getAgentName())
        .ownerId(op.getOwnerId())
        .ownerName(op.getOwnerName())
        .department(op.getDepartment())
        .status(op.getStatus())
        .notes(op.getNotes())
        .closureDate(op.getClosureDate())
        .createdAt(
            op.getCreatedAt() != null
                ? LocalDateTime.ofInstant(op.getCreatedAt(), java.time.ZoneOffset.UTC)
                : null)
        .build();
  }
}
