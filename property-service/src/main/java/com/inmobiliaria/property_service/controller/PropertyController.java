package com.inmobiliaria.property_service.controller;

import com.inmobiliaria.property_service.dto.request.AssignAgentRequest;
import com.inmobiliaria.property_service.dto.request.PropertyRequest;
import com.inmobiliaria.property_service.dto.response.PropertyResponse;
import com.inmobiliaria.property_service.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public PropertyResponse create(
            @Valid @RequestBody PropertyRequest request,
            @RequestHeader("X-Auth-User-Id") String agentId) {
        return propertyService.create(request, agentId);
    }

    @PostMapping("/{id}/images/upload")
    @PreAuthorize("hasRole('AGENT')")
    public Map<String, String> getUploadUrl(@PathVariable String id) {
        return propertyService.generatePresignedUrl(id);
    }

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