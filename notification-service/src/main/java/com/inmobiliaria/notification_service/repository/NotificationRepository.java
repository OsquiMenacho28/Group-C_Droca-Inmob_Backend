package com.inmobiliaria.notification_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.inmobiliaria.notification_service.domain.NotificationDocument;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    Page<NotificationDocument> findByRecipientId(String recipientId, Pageable pageable);
}