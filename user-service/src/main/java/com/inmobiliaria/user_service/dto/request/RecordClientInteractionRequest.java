package com.inmobiliaria.user_service.dto.request;

import com.inmobiliaria.user_service.domain.ClientInteractionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record RecordClientInteractionRequest(
    @NotBlank String clientId,
    @NotBlank String agentId,
    @NotBlank String propertyId,
    String propertyName,
    String agentName,
    @NotNull ClientInteractionType type,
    Instant occurredAt,
    String detail,
    String subType,
    String referenceId) {}
