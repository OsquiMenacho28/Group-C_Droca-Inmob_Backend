package com.inmobiliaria.operation_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.inmobiliaria.operation_service.domain.OperationDocument;

@Repository
public interface OperationRepository extends MongoRepository<OperationDocument, String> {
  List<OperationDocument> findByStatusAndClosureDateBetween(
      String status, LocalDateTime start, LocalDateTime end);

  List<OperationDocument> findByAgentId(String agentId);

  java.util.Optional<OperationDocument> findByPropertyIdAndStatus(String propertyId, String status);
}
