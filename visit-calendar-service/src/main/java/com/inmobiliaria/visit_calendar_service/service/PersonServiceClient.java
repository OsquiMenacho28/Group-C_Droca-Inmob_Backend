package com.inmobiliaria.visit_calendar_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.inmobiliaria.visit_calendar_service.dto.response.PersonResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonServiceClient {
    private final RestTemplate restTemplate;
    @Value("${person.service.url:http://localhost:8084}")
    private String personServiceUrl;

    public PersonResponse getPersonByAuthUserId(String authUserId) {
        String url = personServiceUrl + "/persons/by-auth/" + authUserId;
        return restTemplate.getForObject(url, PersonResponse.class);
    }
}
