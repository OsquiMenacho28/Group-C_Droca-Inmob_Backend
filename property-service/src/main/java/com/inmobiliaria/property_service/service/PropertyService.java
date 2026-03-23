package com.inmobiliaria.property_service.service;

import com.inmobiliaria.property_service.client.IdentityClient;
import com.inmobiliaria.property_service.domain.*;
import com.inmobiliaria.property_service.dto.request.*;
import com.inmobiliaria.property_service.dto.response.PropertyResponse;
import com.inmobiliaria.property_service.repository.PropertyRepository;
import com.inmobiliaria.property_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final IdentityClient identityClient;

    public List<PropertyResponse> findAll() {
        return propertyRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PropertyResponse findById(String id) {
        return propertyRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Inmueble no encontrado: " + id));
    }

    // HU 1: Registro de Inmueble con estado inicial "DISPONIBLE"
    public PropertyResponse create(PropertyRequest request, String agentId) {
        PropertyDocument property = PropertyDocument.builder()
                .title(request.title())
                .address(request.address())
                .price(request.price())
                .type(request.type())
                .m2(request.m2())
                .rooms(request.rooms())
                .status("DISPONIBLE") // Requerimiento HU1
                .assignedAgentId(agentId)
                .imageUrls(new ArrayList<>())
                .assignmentHistory(new ArrayList<>())
                .priceHistory(new ArrayList<>())
                .build();

        property.setCreatedAt(Instant.now());
        property.setCreatedBy(agentId);

        return mapToResponse(propertyRepository.save(property));
    }

    // HU 1: Generar URL prefirmada (Mock de lógica S3/Cloud)
    public Map<String, String> generatePresignedUrl(String id) {
        // Validar que la propiedad existe
        propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inmueble no encontrado"));

        String fileName = UUID.randomUUID().toString() + ".jpg";
        // Estructura requerida: properties/{propertyId}/images/
        String objectKey = "properties/" + id + "/images/" + fileName;

        // Aquí se integraría el SDK de AWS S3 o MinIO
        String uploadUrl = "http://localhost:9000/bucket/" + objectKey + "?signature=mock_sig";
        String publicUrl = "http://localhost:9000/bucket/" + objectKey;

        return Map.of(
            "uploadUrl", uploadUrl,
            "publicUrl", publicUrl
        );
    }

    // HU 1: Confirmar carga de imágenes
    public PropertyResponse addImages(String id, List<String> urls) {
        PropertyDocument prop = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inmueble no encontrado"));
        
        prop.getImageUrls().addAll(urls);
        prop.setUpdatedAt(Instant.now());
        return mapToResponse(propertyRepository.save(prop));
    }

    // HU 2: Modificar precios con historial
    public PropertyResponse updatePrice(String id, Double newPrice, String adminId) {
        PropertyDocument prop = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inmueble no encontrado"));

        // Registrar en historial: precio anterior, fecha y usuario (HU2)
        PriceHistory history = PriceHistory.builder()
                .oldPrice(prop.getPrice())
                .newPrice(newPrice)
                .changedAt(Instant.now())
                .changedBy(adminId)
                .build();

        prop.getPriceHistory().add(history);
        prop.setPrice(newPrice);
        prop.setUpdatedAt(Instant.now());

        return mapToResponse(propertyRepository.save(prop));
    }

    // HU 3: Asignar agente con validación de estado activo
    public PropertyResponse assignAgent(String id, AssignAgentRequest request, String adminId) {
        PropertyDocument prop = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inmueble no encontrado"));

        // Comunicación con user-service vía Feign para validar agente (HU3)
        var agent = identityClient.findById(request.agentId());
        if (!"ACTIVE".equals(agent.status())) {
            throw new RuntimeException("El agente no está disponible o ha sido dado de baja");
        }

        // Registrar historial de reasignación (HU3)
        if (prop.getAssignedAgentId() != null) {
            prop.getAssignmentHistory().add(new AssignmentHistory(
                prop.getAssignedAgentId(),
                Instant.now(),
                adminId
            ));
        }

        prop.setAssignedAgentId(request.agentId());
        prop.setUpdatedAt(Instant.now());
        return mapToResponse(propertyRepository.save(prop));
    }

    public List<PropertyResponse> findByAgent(String agentId) {
        return propertyRepository.findByAssignedAgentId(agentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PropertyResponse mapToResponse(PropertyDocument doc) {
        return new PropertyResponse(
                doc.getId(), doc.getTitle(), doc.getAddress(), doc.getPrice(),
                doc.getType(), doc.getM2(), doc.getRooms(), doc.getStatus(),
                doc.getAssignedAgentId(), doc.getImageUrls(),
                doc.getAssignmentHistory(), doc.getPriceHistory()
        );
    }
}