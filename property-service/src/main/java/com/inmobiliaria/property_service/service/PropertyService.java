package com.inmobiliaria.property_service.service;

import com.inmobiliaria.property_service.client.IdentityClient;
import com.inmobiliaria.property_service.domain.AssignmentHistory;
import com.inmobiliaria.property_service.domain.PropertyDocument;
import com.inmobiliaria.property_service.dto.request.AssignAgentRequest;
import com.inmobiliaria.property_service.dto.response.PropertyResponse;
import com.inmobiliaria.property_service.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final IdentityClient identityClient;

    public PropertyResponse assignAgent(String propertyId, AssignAgentRequest request, String adminId) {
        PropertyDocument property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Validar si el agente existe y está activo
        var agent = identityClient.findById(request.agentId());
        if (!"ACTIVE".equals(agent.status())) {
            throw new RuntimeException("El agente no está disponible o está dado de baja");
        }

        // Historial de reasignación
        if (property.getAssignedAgentId() != null) {
            property.getAssignmentHistory().add(new AssignmentHistory(
                    property.getAssignedAgentId(),
                    property.getUpdatedAt(),
                    property.getCreatedBy()
            ));
        }

        property.setAssignedAgentId(request.agentId());
        property.setUpdatedAt(Instant.now());
        property.setCreatedBy(adminId); // Guardamos quién hizo el último cambio

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