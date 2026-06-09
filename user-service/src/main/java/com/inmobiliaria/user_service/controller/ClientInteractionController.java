package com.inmobiliaria.user_service.controller;

import com.inmobiliaria.user_service.domain.ClientInteractionType;
import com.inmobiliaria.user_service.dto.request.RecordClientInteractionRequest;
import com.inmobiliaria.user_service.dto.response.ApiResponse;
import com.inmobiliaria.user_service.dto.response.ClientInteractionResponse;
import com.inmobiliaria.user_service.dto.response.ResponseFactory;
import com.inmobiliaria.user_service.service.ClientInteractionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientInteractionController {

  private final ClientInteractionService clientInteractionService;
  private final ResponseFactory responseFactory;

  @GetMapping("/{clientAuthUserId}/interactions")
  @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'INTERESTED_CLIENT', 'EMPLOYEE')")
  public ResponseEntity<ApiResponse<List<ClientInteractionResponse>>> getClientInteractions(
      @PathVariable String clientAuthUserId,
      @RequestParam(required = false) ClientInteractionType type,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      HttpServletRequest request) {

    String requesterId = request.getHeader("X-Auth-User-Id");
    Instant fromInstant =
        from != null && !from.isBlank() ? ClientInteractionService.parseDateStart(from) : null;
    Instant toInstant =
        to != null && !to.isBlank() ? ClientInteractionService.parseDateEnd(to) : null;

    List<ClientInteractionResponse> data =
        clientInteractionService.getClientInteractions(
            clientAuthUserId, requesterId, type, fromInstant, toInstant);

    return ResponseEntity.ok(
        responseFactory.success("Client interactions retrieved successfully", data));
  }

  @PostMapping("/interactions")
  public ResponseEntity<ApiResponse<ClientInteractionResponse>> recordInteraction(
      @Valid @RequestBody RecordClientInteractionRequest body) {

    ClientInteractionResponse data = clientInteractionService.recordInteraction(body);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(responseFactory.created("Client interaction recorded successfully", data));
  }
}
