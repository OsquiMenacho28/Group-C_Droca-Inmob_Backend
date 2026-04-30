package com.inmobiliaria.operation_service.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.inmobiliaria.operation_service.domain.OperationDocument;
import com.inmobiliaria.operation_service.domain.ReceiptDocument;
import com.inmobiliaria.operation_service.dto.ReceiptResponse;
import com.inmobiliaria.operation_service.dto.ReceiptUploadRequest;
import com.inmobiliaria.operation_service.exception.ResourceNotFoundException;
import com.inmobiliaria.operation_service.repository.OperationRepository;
import com.inmobiliaria.operation_service.repository.ReceiptRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

  private final ReceiptRepository receiptRepository;
  private final OperationRepository operationRepository;
  private final MinioStorageService minioStorageService;

  public ReceiptResponse attachReceipt(
      String operationId,
      String userId,
      String rolesHeader,
      MultipartFile file,
      ReceiptUploadRequest request) {

    OperationDocument operation =
        operationRepository
            .findById(operationId)
            .orElseThrow(() -> new ResourceNotFoundException("Operation not found"));

    if (!isRelatedUser(operation, userId, rolesHeader)) {
      throw new com.inmobiliaria.operation_service.exception.ValidationException(
          "You do not have permission to upload receipts for this operation.");
    }

    // if (!"PENDING".equalsIgnoreCase(operation.getStatus())) {
    //   throw new IllegalArgumentException(
    //       "Receipts can only be uploaded while the property is reserved.");
    // }

    // 1. Upload to MinIO
    String minioPath = minioStorageService.uploadFile(file, operationId);

    ReceiptDocument doc =
        ReceiptDocument.builder()
            .operationId(operationId)
            .fileName(file.getOriginalFilename())
            .contentType(file.getContentType())
            .size(file.getSize())
            .minioObjectPath(minioPath)
            .amount(request.getAmount().doubleValue())
            .currency(request.getCurrency())
            .notes(request.getConcept())
            .paymentDate(request.getPaymentDate()) // Store the actual payment date
            .build();

    doc.setCreatedAt(Instant.now());
    doc.setCreatedBy(userId);

    ReceiptDocument saved = receiptRepository.save(doc);
    return mapToResponse(saved);
  }

  public List<ReceiptResponse> listReceipts(String operationId) {
    return receiptRepository.findByOperationId(operationId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public void deleteReceipt(
      String operationId, String receiptId, String userId, String rolesHeader) {
    ReceiptDocument doc =
        receiptRepository
            .findById(receiptId)
            .orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));

    if (!doc.getOperationId().equals(operationId)) {
      throw new IllegalArgumentException("Receipt does not belong to this operation");
    }

    // Permission Check: Only Admin or the Agent assigned to this operation can delete
    OperationDocument operation =
        operationRepository
            .findById(operationId)
            .orElseThrow(() -> new ResourceNotFoundException("Operation not found"));

    if (!isAdminOrAssignedAgent(operation, userId, rolesHeader)) {
      throw new com.inmobiliaria.operation_service.exception.ValidationException(
          "You do not have permission to delete this receipt.");
    }

    // Delete from MinIO
    minioStorageService.deleteFile(doc.getMinioObjectPath());

    // Delete from Mongo
    receiptRepository.delete(doc);
  }

  private boolean isRelatedUser(OperationDocument operation, String userId, String rolesHeader) {
    return hasAdminRole(rolesHeader)
        || userId.equals(operation.getAgentId())
        || userId.equals(operation.getOwnerId())
        || userId.equals(operation.getClientId());
  }

  private boolean isAdminOrAssignedAgent(
      OperationDocument operation, String userId, String rolesHeader) {
    return hasAdminRole(rolesHeader) || userId.equals(operation.getAgentId());
  }

  private boolean hasAdminRole(String rolesHeader) {
    if (rolesHeader == null || rolesHeader.isBlank()) {
      return false;
    }
    return Arrays.stream(rolesHeader.replace("[", "").replace("]", "").split(","))
        .map(String::trim)
        .map(String::toUpperCase)
        .anyMatch(role -> role.equals("ADMIN") || role.equals("ROLE_ADMIN"));
  }

  private ReceiptResponse mapToResponse(ReceiptDocument doc) {
    String downloadUrl = minioStorageService.generatePresignedUrl(doc.getMinioObjectPath());

    return ReceiptResponse.builder()
        .id(doc.getId())
        .operationId(doc.getOperationId())
        .fileName(doc.getFileName())
        .contentType(doc.getContentType())
        .size(doc.getSize())
        .amount(java.math.BigDecimal.valueOf(doc.getAmount()))
        .currency(doc.getCurrency())
        .concept(doc.getNotes())
        .paymentDate(doc.getPaymentDate())
        .createdBy(doc.getCreatedBy())
        .createdAt(
            doc.getCreatedAt() != null
                ? LocalDateTime.ofInstant(doc.getCreatedAt(), ZoneOffset.UTC)
                : null)
        .downloadUrl(downloadUrl)
        .build();
  }
}
