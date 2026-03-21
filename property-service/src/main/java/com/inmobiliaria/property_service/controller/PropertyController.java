package com.inmobiliaria.property_service.controller;

import com.inmobiliaria.property_service.dto.request.AssignAgentRequest;
import com.inmobiliaria.property_service.dto.response.PropertyResponse;
import com.inmobiliaria.property_service.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PatchMapping("/{id}/assign-agent")
    @PreAuthorize("hasRole('ADMIN')")
    public PropertyResponse assignAgent(
            @PathVariable String id,
            @Valid @RequestBody AssignAgentRequest request,
            @RequestHeader("X-Auth-User-Id") String adminId) {
        return propertyService.assignAgent(id, request, adminId);
    }

    @GetMapping("/agent/{agentId}")
    public List<PropertyResponse> getByAgent(@PathVariable String agentId) {
        return propertyService.findByAgent(agentId);
    }
}