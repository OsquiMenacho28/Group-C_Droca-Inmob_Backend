package com.inmobiliaria.user_service.dto.response;

import com.inmobiliaria.user_service.domain.ClientInteractionType;
import java.time.Instant;

public record ClientInteractionResponse(
    String id,
    String clientId,
    String agentId,
    String agentName,
    String propertyId,
    String propertyName,
    ClientInteractionType type,
    Instant occurredAt,
    String detail,
    String subType) {}
