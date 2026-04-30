package com.inmobiliaria.visit_calendar_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.inmobiliaria.visit_calendar_service.dto.response.PropertyResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropertyServiceClient {
    private final RestTemplate restTemplate;
    @Value("${property.service.url:http://localhost:8085}")
    private String propertyServiceUrl;

    public PropertyResponse getPropertyById(String propertyId) {
        String url = propertyServiceUrl + "/properties/" + propertyId;
        return restTemplate.getForObject(url, PropertyResponse.class);
    }
}