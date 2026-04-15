package com.inmobiliaria.notification_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.inmobiliaria.notification_service.domain.EmailLogDocument;

public interface EmailLogRepository extends MongoRepository<EmailLogDocument, String> {}
