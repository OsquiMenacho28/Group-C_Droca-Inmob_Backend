package com.inmobiliaria.operation_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.inmobiliaria.operation_service.domain.ReceiptDocument;

@Repository
public interface ReceiptRepository extends MongoRepository<ReceiptDocument, String> {
  List<ReceiptDocument> findByOperationId(String operationId);
}
