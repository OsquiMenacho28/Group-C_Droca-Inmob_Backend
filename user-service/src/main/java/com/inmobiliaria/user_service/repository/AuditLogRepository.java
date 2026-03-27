package com.inmobiliaria.user_service.repository;

import com.inmobiliaria.user_service.domain.AuditLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Sort;
import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLogDocument, String> {
    List<AuditLogDocument> findAll(Sort sort);
}