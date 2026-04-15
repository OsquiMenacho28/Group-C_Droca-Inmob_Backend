package com.inmobiliaria.identity_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.inmobiliaria.identity_service.domain.AuditLog;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {}
