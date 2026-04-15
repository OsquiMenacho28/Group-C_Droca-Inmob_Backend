package com.inmobiliaria.notification_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inmobiliaria.notification_service.dto.request.SendCredentialsEmailRequest;
import com.inmobiliaria.notification_service.dto.response.ApiResponse;
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

  @PostMapping("/credentials")
  public ResponseEntity<ApiResponse<NotificationResponse>> sendCredentials(
      @Valid @RequestBody SendCredentialsEmailRequest request) {
    NotificationResponse response = notificationService.sendCredentialsEmail(request);
    return ResponseEntity.ok(
        responseFactory.success("Credentials email sent successfully", response));
  }
}
