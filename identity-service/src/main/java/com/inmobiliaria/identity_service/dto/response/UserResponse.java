package com.inmobiliaria.identity_service.dto.response;

import java.time.Instant;
import java.util.List;

import com.inmobiliaria.identity_service.domain.UserStatus;
import com.inmobiliaria.identity_service.domain.UserType;

public record UserResponse(
    String id,
    String firstName,
    String lastName,
    String fullName,
    String email,
    UserType userType,
    UserStatus status,
    Boolean temporaryPassword,
    Instant temporaryPasswordExpiresAt,
    Boolean mustChangePassword,
    List<String> primaryRoleIds) {}
