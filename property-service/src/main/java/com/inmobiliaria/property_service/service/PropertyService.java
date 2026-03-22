package com.inmobiliaria.property_service.service;

import com.inmobiliaria.property_service.client.IdentityClient;
import com.inmobiliaria.property_service.domain.AssignmentHistory;
import com.inmobiliaria.property_service.domain.PropertyDocument;
import com.inmobiliaria.property_service.dto.request.AssignAgentRequest;
import com.inmobiliaria.property_service.dto.request.PropertyRequest;
import com.inmobiliaria.property_service.dto.response.PropertyResponse;
import com.inmobiliaria.property_service.repository.PropertyRepository;
import com.inmobiliaria.property_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final IdentityClient identityClient;

    public PropertyResponse create(PropertyRequest request, String agentId) {
        PropertyDocument property = PropertyDocument.builder()
                .title(request.title())
                .address(request.address())
                .price(request.price())
                .type(request.type())
                .m2(request.m2())
                .rooms(request.rooms())
                .status("DISPONIBLE") // Requerimiento: Estado inicial automático
                .assignedAgentId(agentId)
                .build();

        property.setCreatedAt(Instant.now());
        property.setUpdatedAt(Instant.now());
        property.setCreatedBy(agentId);

        return mapToResponse(propertyRepository.save(property));
    }

    public Map<String, String> generatePresignedUrl(String propertyId) {
        String fileName = "prop-" + propertyId + "-" + UUID.randomUUID() + ".jpg";
        String bucketPath = "properties/" + propertyId + "/images/" + fileName;
        
        // Simulación de URL prefirmada para carga directa
        return Map.of(
            "uploadUrl", "https://s3.amazonaws.com/inmobiliaria-bucket/" + bucketPath + "?X-Amz-Signature=...",
            "publicUrl", "https://cdn.inmobiliaria.com/" + bucketPath
        );
    }

    public PropertyResponse assignAgent(String propertyId, AssignAgentRequest request, String adminId) {
        PropertyDocument property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        var agent = identityClient.findById(request.agentId());
        if (!"ACTIVE".equals(agent.status())) {
            throw new RuntimeException("El agente no está activo");
        }

        if (property.getAssignedAgentId() != null) {
            property.getAssignmentHistory().add(new AssignmentHistory(
                    property.getAssignedAgentId(),
                    property.getUpdatedAt(),
                    property.getCreatedBy()
            ));
        }

        property.setAssignedAgentId(request.agentId());
        property.setUpdatedAt(Instant.now());
        property.setCreatedBy(adminId);

        return mapToResponse(propertyRepository.save(property));
    }

    public List<PropertyResponse> findByAgent(String agentId) {
        return propertyRepository.findByAssignedAgentId(agentId).stream()
                .map(this::mapToResponse).toList();
    }

    private PropertyResponse mapToResponse(PropertyDocument doc) {
        return new PropertyResponse(doc.getId(), doc.getTitle(), doc.getAddress(),
                                    doc.getPrice(), doc.getAssignedAgentId());
    }
}