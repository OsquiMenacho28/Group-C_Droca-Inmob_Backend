package com.inmobiliaria.user_service.dto.response;

import com.inmobiliaria.user_service.domain.PersonType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PersonResponse(
        String id,
        String authUserId,
        String firstName,
        String lastName,
        String fullName,
        LocalDate birthDate,
        String phone,
        String email,
        PersonType personType,
        List<String> roleIds,
        boolean customRole,
        
        // Type-specific (optional)
        String department,
        String position,
        Instant hireDate,
        String taxId,
        String preferredContactMethod,
        String budget
) {}
