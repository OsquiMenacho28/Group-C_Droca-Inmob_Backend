package com.inmobiliaria.notification_service.repository;

import com.inmobiliaria.notification_service.domain.NotificationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
  Page<NotificationDocument> findByRecipientId(String recipientId, Pageable pageable);
}
