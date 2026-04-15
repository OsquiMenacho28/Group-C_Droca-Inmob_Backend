package com.inmobiliaria.identity_service.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record AssignRoleRequest(@NotEmpty List<String> roleIds) {}
