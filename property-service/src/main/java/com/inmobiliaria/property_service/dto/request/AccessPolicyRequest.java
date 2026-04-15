package com.inmobiliaria.property_service.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

public record AccessPolicyRequest(
    @NotNull(message = "La política de acceso no puede ser nula") Set<String> accessPolicy) {}
