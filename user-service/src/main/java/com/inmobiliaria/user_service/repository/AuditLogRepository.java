package com.inmobiliaria.user_service.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.inmobiliaria.user_service.domain.AuditLogDocument;

public interface AuditLogRepository extends MongoRepository<AuditLogDocument, String> {
  List<AuditLogDocument> findAll(Sort sort);
}
