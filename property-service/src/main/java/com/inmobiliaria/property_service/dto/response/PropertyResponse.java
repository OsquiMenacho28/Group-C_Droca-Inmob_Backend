package com.inmobiliaria.property_service.dto.response;

public record PropertyResponse(
        String id,
        String title,
        String address,
        Double price,
        String assignedAgentId
) {}