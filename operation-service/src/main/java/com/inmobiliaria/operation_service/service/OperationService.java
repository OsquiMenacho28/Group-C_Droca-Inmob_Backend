package com.inmobiliaria.operation_service.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.inmobiliaria.operation_service.client.PropertyClient;
import com.inmobiliaria.operation_service.domain.OperationDocument;
import com.inmobiliaria.operation_service.dto.OperationRequest;
import com.inmobiliaria.operation_service.dto.OperationResponse;
import com.inmobiliaria.operation_service.exception.ResourceNotFoundException;
import com.inmobiliaria.operation_service.exception.ValidationException;
import com.inmobiliaria.operation_service.repository.OperationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationService {

  private final OperationRepository operationRepository;
  private final PropertyClient propertyClient;

  public List<OperationResponse> findAll(String userId, String rolesHeader) {
    log.info(
        "[OperationService] Finding operations. User: {}, RolesHeader: {}", userId, rolesHeader);

    if (rolesHeader == null || rolesHeader.isBlank()) {
      log.warn("[OperationService] No roles provided for user: {}", userId);
      return new ArrayList<>();
    }

    // Clean and normalize roles: "[ROLE_ADMIN, AGENT]" -> ["ROLE_ADMIN", "AGENT"]
    String cleanRoles =
        rolesHeader.replace("[", "").replace("]", "").replace(" ", "").toUpperCase();
    List<String> roles = Arrays.asList(cleanRoles.split(","));
    log.debug("[OperationService] Normalized roles: {}", roles);

    // Check for Admin (flexible check)
    if (roles.contains("ROLE_ADMIN") || roles.contains("ADMIN")) {
      log.info("[OperationService] User {} has ADMIN privileges. Fetching all operations.", userId);
      return operationRepository.findAll().stream()
          .sorted(
              (a, b) -> {
                if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
              })
          .map(this::mapToResponse)
          .collect(Collectors.toList());
    }

    Set<OperationDocument> combinedOperations = new HashSet<>();

    // Check for Agent
    if (roles.contains("ROLE_AGENT") || roles.contains("AGENT")) {
      List<OperationDocument> agentOps = operationRepository.findByAgentId(userId);
      log.debug("[OperationService] Found {} operations for agent: {}", agentOps.size(), userId);
      combinedOperations.addAll(agentOps);
    }

    // Owner and Client check (based on ID match in document)
    List<OperationDocument> ownerOps = operationRepository.findByOwnerId(userId);
    List<OperationDocument> clientOps = operationRepository.findByClientId(userId);

    log.debug(
        "[OperationService] ID matches -> Owner: {}, Client: {}",
        ownerOps.size(),
        clientOps.size());

    combinedOperations.addAll(ownerOps);
    combinedOperations.addAll(clientOps);

    log.info(
        "[OperationService] Total unique operations for user {}: {}",
        userId,
        combinedOperations.size());

    return combinedOperations.stream()
        .sorted(
            (a, b) -> {
              if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
              return b.getCreatedAt().compareTo(a.getCreatedAt());
            })
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

  public OperationResponse updateStatus(
      String id, String status, String userId, String rolesHeader) {
    // Buscar la operación una sola vez
    OperationDocument operation =
        operationRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Operation not found with id: " + id));

    String oldStatus = operation.getStatus();

    // Normalizar roles - agregar import org.springframework.http.HttpStatus si es necesario
    if (rolesHeader == null) rolesHeader = "";
    String cleanRoles =
        rolesHeader.replace("[", "").replace("]", "").replace(" ", "").toUpperCase();
    List<String> rawRoles = Arrays.asList(cleanRoles.split(","));

    // Normalizar roles (asegurar que tengan prefijo ROLE_)
    List<String> normalizedRoles =
        rawRoles.stream()
            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
            .collect(Collectors.toList());

    boolean isAdmin = normalizedRoles.contains("ROLE_ADMIN");
    boolean isAssignedAgent = userId.equals(operation.getAgentId());

    if (!isAdmin && !isAssignedAgent) {
      throw new ValidationException(
          "Only Admins or the assigned Agent can update the operation status.");
    }

    // Validaciones de estado
    if ("CLOSED".equalsIgnoreCase(oldStatus) && !"CANCELLED".equalsIgnoreCase(status)) {
      throw new ValidationException(
          "Esta operación ya ha sido CERRADA de forma definitiva y no puede ser modificada.");
    }

    if ("CANCELLED".equalsIgnoreCase(oldStatus)) {
      throw new ValidationException("La operación ya ha sido cancelada y no puede ser reactivada.");
    }

    // Actualizar estado
    operation.setStatus(status);
    operation.setUpdatedAt(Instant.now());
    OperationDocument saved = operationRepository.save(operation);

    try {
      syncPropertyStatus(saved.getPropertyId(), status, userId);
    } catch (Exception e) {
      log.error("Failed to sync status, rolling back status update: {}", e.getMessage());
      operation.setStatus(oldStatus);
      operationRepository.save(operation);
      throw new ValidationException(
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
