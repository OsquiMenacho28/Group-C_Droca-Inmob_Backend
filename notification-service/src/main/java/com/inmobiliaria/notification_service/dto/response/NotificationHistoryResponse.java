// notification-service/dto/response/NotificationHistoryResponse.java
package com.inmobiliaria.notification_service.dto.response;

import java.time.LocalDateTime;

import com.inmobiliaria.notification_service.domain.NotificationStatus;

public record NotificationHistoryResponse(
    String id,
    String type,
    String subject,
    String content,
    NotificationStatus status,
    LocalDateTime createdAt,
    LocalDateTime sentAt,
    String errorMessage) {}
