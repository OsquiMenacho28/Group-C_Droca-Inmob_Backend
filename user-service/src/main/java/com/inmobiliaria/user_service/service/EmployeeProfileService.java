package com.inmobiliaria.user_service.service;

import com.inmobiliaria.user_service.domain.PersonType;
import com.inmobiliaria.user_service.dto.request.CreateEmployeeRequest;
import com.inmobiliaria.user_service.dto.request.CreatePersonRequest;
import com.inmobiliaria.user_service.dto.response.PersonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeProfileService {
    private final PersonService personService;

    public PersonResponse createEmployeeProfile(CreateEmployeeRequest request) {
        CreatePersonRequest genericRequest = new CreatePersonRequest(
                request.authUserId(),
                request.firstName(),
                request.lastName(),
                request.birthDate(),
                request.phone(),
                request.email(),
                PersonType.EMPLOYEE,
                null, // roles handled separately
                request.department(),
                request.position(),
                request.hireDate(),
                null, null, null
        );
        return personService.create(genericRequest);
    }
}
