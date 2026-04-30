package com.inmobiliaria.notification_service.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// backend/notification-service/src/main/java/.../domain/NotificationDocument.java
@Document(collection = "notifications")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationDocument {
    @Id private String id;
    private String recipientId;          // ID del propietario/usuario
    private String type;                 // ej: "VISIT_SCHEDULED", "CREDENTIALS"
    private String channel;              // "EMAIL", "PUSH" (futuro)
    private String subject;
    private String content;
    private NotificationStatus status;   // PENDING, SENT, FAILED
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}