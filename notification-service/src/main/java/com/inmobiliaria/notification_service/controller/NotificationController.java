package com.inmobiliaria.notification_service.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inmobiliaria.notification_service.domain.NotificationDocument;
import com.inmobiliaria.notification_service.domain.NotificationStatus;
import com.inmobiliaria.notification_service.repository.NotificationRepository;
import com.inmobiliaria.notification_service.service.NotificationDispatcher;
import com.inmobiliaria.notification_service.dto.request.SendCredentialsEmailRequest;
import com.inmobiliaria.notification_service.dto.request.SendNotificationRequest;
import com.inmobiliaria.notification_service.dto.response.ApiResponse;
import com.inmobiliaria.notification_service.dto.response.NotificationHistoryResponse;
import com.inmobiliaria.notification_service.dto.response.NotificationResponse;
import com.inmobiliaria.notification_service.dto.response.ResponseFactory;
import com.inmobiliaria.notification_service.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final ResponseFactory responseFactory;
  private final NotificationRepository notificationRepository;
  private final NotificationDispatcher notificationDispatcher;

  @PostMapping("/credentials")
  public ResponseEntity<ApiResponse<NotificationResponse>> sendCredentials(
      @Valid @RequestBody SendCredentialsEmailRequest request) {
    NotificationResponse response = notificationService.sendCredentialsEmail(request);
    return ResponseEntity.ok(
        responseFactory.success("Credentials email sent successfully", response));
  }

    // controller/NotificationController.java
  @PostMapping("/send")
  public ResponseEntity<ApiResponse<Void>> sendNotification(@Valid @RequestBody SendNotificationRequest req) {
      NotificationDocument doc = NotificationDocument.builder()
          .recipientId(req.recipientId())
          .type(req.type())
          .channel(req.channel() != null ? req.channel() : "EMAIL")
          .subject(req.subject())
          .content(req.content())
          .status(NotificationStatus.PENDING)
          .retryCount(0)
          .createdAt(LocalDateTime.now())
          .build();
      notificationDispatcher.send(doc);  // asíncrono
      return ResponseEntity.accepted().body(responseFactory.success("Notificación encolada"));
  }

  @GetMapping("/propietarios/{ownerId}/notificaciones")
  public ResponseEntity<ApiResponse<List<NotificationHistoryResponse>>> getNotificationsByOwner(
          @PathVariable String ownerId,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size) {
      Page<NotificationDocument> pageResult = notificationRepository.findByRecipientId(ownerId, PageRequest.of(page, size));
      List<NotificationHistoryResponse> list = pageResult.map(this::toHistoryResponse).toList();
      return ResponseEntity.ok(responseFactory.paginated("Historial de notificaciones", list, page, size, pageResult.getTotalElements()));
  }
  private NotificationHistoryResponse toHistoryResponse(NotificationDocument doc) {
    return new NotificationHistoryResponse(
        doc.getId(),
        doc.getType(),
        doc.getSubject(),
        doc.getContent(),
        doc.getStatus(),
        doc.getCreatedAt(),
        doc.getSentAt(),
        doc.getErrorMessage()
    );
}
}
