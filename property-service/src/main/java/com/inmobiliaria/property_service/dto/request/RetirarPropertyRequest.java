package com.inmobiliaria.property_service.dto.request;

import com.inmobiliaria.property_service.domain.RetiroMotivo;

import jakarta.validation.constraints.NotNull;

public record RetirarPropertyRequest(
    @NotNull(message = "El motivo de retiro es obligatorio")
    RetiroMotivo motivoRetiro,
    String detalleRetiro  // opcional
) {}
