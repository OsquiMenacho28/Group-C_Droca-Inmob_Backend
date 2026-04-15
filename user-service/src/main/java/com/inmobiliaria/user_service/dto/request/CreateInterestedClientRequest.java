package com.inmobiliaria.user_service.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInterestedClientRequest(
    @NotBlank String authUserId,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotNull LocalDate birthDate,
    @NotBlank String phone,
    @NotBlank String email,
    String preferredContactMethod,
    String budget) {}
