package com.inmobiliaria.property_service.service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.inmobiliaria.property_service.client.IdentityClient;
import com.inmobiliaria.property_service.client.OperationClient;
import com.inmobiliaria.property_service.domain.*;
import com.inmobiliaria.property_service.dto.request.*;
import com.inmobiliaria.property_service.dto.response.PropertyResponse;
import com.inmobiliaria.property_service.dto.response.ResponsableResponse;
import com.inmobiliaria.property_service.exception.AccessDeniedException;
import com.inmobiliaria.property_service.exception.ResourceNotFoundException;
import com.inmobiliaria.property_service.exception.ValidationException;
import com.inmobiliaria.property_service.repository.PropertyRepository;
import com.inmobiliaria.property_service.security.Auditable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {

  private final PropertyRepository propertyRepository;
  private final IdentityClient identityClient;
  private final OperationClient operationClient;
  private final MongoTemplate mongoTemplate;
  private final ImageService imageService;

  /** Búsqueda avanzada con filtros dinámicos y seguridad por rol. */
  public Map<String, Object> findWithFilters(
      String title,
      String type,
      String zone,
      String status,
      OperationType operationType,
      Double minPrice,
      Double maxPrice,
      String agentId,
      String currentUserId,
      List<String> roles,
      String sortBy,
      String sortOrder,
      int page,
      int pageSize) {

    Query query = new Query();
    List<Criteria> filters = new ArrayList<>();

    filters.add(Criteria.where("deleted").is(false));

    if (title != null && !title.isBlank()) {
      filters.add(Criteria.where("title").regex(title, "i"));
    }
    if (type != null && !type.isBlank()) {
      filters.add(Criteria.where("type").is(type));
    }
    if (zone != null && !zone.isBlank()) {
      filters.add(Criteria.where("zone").is(zone));
    }

    if (status != null && !status.isBlank()) {
      filters.add(Criteria.where("status").is(PropertyStatus.valueOf(status.toUpperCase())));
    } else {
      // DEFAULT: Hide VENDIDO and ELIMINADO properties from main listings unless explicitly
      // requested
      filters.add(Criteria.where("status").nin(PropertyStatus.VENDIDO, PropertyStatus.ELIMINADO));

      if (!roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_AGENT")) {
        // FOR CLIENTS: By default only show available or almost available ones
        filters.add(
            Criteria.where("status")
                .in(
                    PropertyStatus.DISPONIBLE,
                    PropertyStatus.RESERVADO,
                    PropertyStatus.EN_NEGOCIACION,
                    PropertyStatus.CONTRACTED));
      }
    }

    if (operationType != null) {
      filters.add(Criteria.where("operationType").is(operationType));
    }
    if (minPrice != null) {
      filters.add(Criteria.where("price").gte(minPrice));
    }
    if (maxPrice != null) {
      filters.add(Criteria.where("price").lte(maxPrice));
    }
    if (agentId != null && !agentId.isBlank()) {
      filters.add(Criteria.where("assignedAgentId").is(agentId));
    }

    // Seguridad: Si no es ADMIN, solo ve sus asignadas, las permitidas por política (PUBLIC por
    // defecto) o cualquier propiedad disponible/en curso (HU: Visibilidad pública)
    if (!roles.contains("ROLE_ADMIN")) {
      Criteria securityCriteria =
          new Criteria()
              .orOperator(
                  Criteria.where("assignedAgentId").is(currentUserId),
                  Criteria.where("ownerId").is(currentUserId),
                  Criteria.where("accessPolicy")
                      .in(
                          currentUserId,
                          "ROLE_AGENT",
                          "PUBLIC",
                          "ROLE_USER",
                          "rol_interested_client"),
                  Criteria.where("status")
                      .in(
                          PropertyStatus.DISPONIBLE,
                          PropertyStatus.RESERVADO,
                          PropertyStatus.EN_NEGOCIACION,
                          PropertyStatus.CONTRACTED));
      filters.add(securityCriteria);
    }

    if (!filters.isEmpty()) {
      query.addCriteria(new Criteria().andOperator(filters.toArray(new Criteria[0])));
    }

    Sort.Direction direction =
        "DESC".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
    String sortField =
        switch (sortBy) {
          case "title" -> "title";
          case "m2" -> "m2";
          case "rooms" -> "rooms";
          case "status" -> "status";
          default -> "price";
        };

    long total = mongoTemplate.count(query, PropertyDocument.class);

    query.with(Sort.by(direction, sortField)).skip((long) page * pageSize).limit(pageSize);

    List<PropertyResponse> data =
        mongoTemplate.find(query, PropertyDocument.class).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("data", data);
    result.put("page", page);
    result.put("pageSize", pageSize);
    result.put("totalElements", total);
    result.put("totalPages", (int) Math.ceil((double) total / pageSize));
    return result;
  }

  @Auditable(action = "PROPERTY_UPDATE") // Los agentes también auditan su update
  public PropertyResponse updatePropertyAsAgent(
      String id, AgentPropertyUpdateRequest request, String agentId) {
    PropertyDocument property =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

    // Verify the agent is assigned to this property
    boolean isAssignedAgent =
        property.getAssignedAgentId() != null && property.getAssignedAgentId().equals(agentId);

    if (!isAssignedAgent) {
      throw new AccessDeniedException("You can only update properties assigned to you");
    }

    // Update allowed fields (price is NOT included)
    property.setTitle(request.title());
    property.setAddress(request.address());
    property.setZone(request.zone());
    property.setType(request.type());
    property.setM2(request.m2());
    property.setRooms(request.rooms());
    property.setOperationType(request.operationType());

    if (request.ownerId() != null && !request.ownerId().isBlank()) {
      property.setOwnerId(request.ownerId());
    }

    property.setUpdatedAt(Instant.now());
    property.setCreatedBy(agentId);

    propertyRepository.save(property);

    log.info("Property {} updated by agent {}", id, agentId);

    return mapToResponse(propertyRepository.findById(id).orElseThrow());
  }

  public List<PropertyResponse> findAll() {
    return propertyRepository.findAll().stream().map(this::mapToResponse).toList();
  }

  public PropertyResponse findById(String id) {
    return propertyRepository
        .findById(id)
        .map(this::mapToResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Inmueble no encontrado: " + id));
  }

  public ResponsableResponse getResponsable(String propertyId) {
    PropertyDocument property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Inmueble no encontrado: " + propertyId));

    String agentId = property.getAssignedAgentId();
    if (agentId == null || agentId.isBlank()) {
      throw new ResourceNotFoundException("Este inmueble no tiene agente responsable asignado");
    }

    try {
      IdentityClient.UserResponse agent = identityClient.findById(agentId);
      if (agent == null) {
        throw new ResourceNotFoundException("No se pudo obtener información del responsable");
      }
      return new ResponsableResponse(
          agent.id(),
          agent.fullName() != null
              ? agent.fullName()
              : (agent.firstName() + " " + agent.lastName()).trim(),
          agent.email(),
          agent.phone());
    } catch (Exception e) {
      log.warn("No se pudo obtener datos del responsable {}: {}", agentId, e.getMessage());
      throw new ResourceNotFoundException("No se pudo obtener información del responsable");
    }
  }

  @Auditable(action = "PROPERTY_CREATE")
  public PropertyResponse create(PropertyRequest request, String agentId) {
    PropertyDocument property =
        PropertyDocument.builder()
            .title(request.title())
            .address(request.address())
            .zone(request.zone())
            .price(request.price())
            .type(request.type())
            .operationType(request.operationType())
            .m2(request.m2())
            .rooms(request.rooms())
            .status(PropertyStatus.DISPONIBLE)
            .assignedAgentId(agentId)
            .ownerId(request.ownerId())
            .accessPolicy(
                request.accessPolicy() != null && !request.accessPolicy().isEmpty()
                    ? request.accessPolicy()
                    : new HashSet<>(List.of("PUBLIC")))
            .build();
    property.setCreatedAt(Instant.now());
    property.setCreatedBy(agentId);
    return mapToResponse(propertyRepository.save(property));
  }

  @Auditable(action = "PRICE_UPDATE")
  public PropertyResponse updatePrice(String id, Double newPrice, String adminId) {
    PropertyDocument prop = propertyRepository.findById(id).orElseThrow();
    prop.getPriceHistory().add(new PriceHistory(prop.getPrice(), newPrice, Instant.now(), adminId));
    prop.setPrice(newPrice);
    return mapToResponse(propertyRepository.save(prop));
  }

  @Auditable(action = "AGENT_ASSIGN")
  public PropertyResponse assignAgent(String id, AssignAgentRequest request, String adminId) {
    PropertyDocument prop = propertyRepository.findById(id).orElseThrow();
    var agent = identityClient.findById(request.agentId());
    if (agent == null || !"ACTIVE".equals(agent.status()))
      throw new RuntimeException("Agente inactivo");
    prop.getAssignmentHistory()
        .add(new AssignmentHistory(prop.getAssignedAgentId(), Instant.now(), adminId));
    prop.setAssignedAgentId(request.agentId());

    // --- NUEVO: Asignar el agente al dueño de la propiedad ---
    if (prop.getOwnerId() != null) {
      try {
        identityClient.updateUser(
            prop.getOwnerId(),
            new IdentityClient.UpdateUserRequest(
                null, null, null, null, null, null, null, null, null, null, request.agentId()));
        log.info("Agent {} assigned to owner {}", request.agentId(), prop.getOwnerId());
      } catch (Exception e) {
        log.warn("Failed to assign agent to owner {}: {}", prop.getOwnerId(), e.getMessage());
      }
    }

    return mapToResponse(propertyRepository.save(prop));
  }

  public List<PropertyResponse> findByAgent(String agentId) {
    return propertyRepository.findByAssignedAgentId(agentId).stream()
        .map(this::mapToResponse)
        .toList();
  }

  public List<PropertyResponse> findByOwner(String ownerId) {
    return propertyRepository.findByOwnerId(ownerId).stream().map(this::mapToResponse).toList();
  }

  @Auditable(action = "OWNER_ASSIGN")
  public PropertyResponse assignOwner(String id, String ownerId, String adminId) {
    PropertyDocument prop =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

    // Optional: Validate owner exists in identity service
    try {
      var owner = identityClient.findById(ownerId);
      if (owner == null || !"ACTIVE".equals(owner.status())) {
        throw new ValidationException("Owner is not active");
      }
    } catch (Exception e) {
      log.warn("Could not validate owner: {}", e.getMessage());
    }

    prop.setOwnerId(ownerId);
    prop.setUpdatedAt(Instant.now());

    // Add to audit/assignment history if needed
    if (prop.getAssignmentHistory() == null) {
      prop.setAssignmentHistory(new ArrayList<>());
    }
    prop.getAssignmentHistory()
        .add(new AssignmentHistory(prop.getAssignedAgentId(), Instant.now(), adminId));

    return mapToResponse(propertyRepository.save(prop));
  }

  // ... (dentro de la clase PropertyService)

  @Auditable(action = "LOCATION_UPDATE")
  public PropertyResponse updateLocation(
      String id, UpdateLocationRequest request, String userId, List<String> roles) {
    PropertyDocument property =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

    // Validar permisos (Admin o Agente asignado)
    boolean isAdmin = roles.contains("ROLE_ADMIN");
    boolean isAssignedAgent = userId.equals(property.getAssignedAgentId());

    if (!isAdmin && !isAssignedAgent) {
      throw new AccessDeniedException(
          "No tiene permisos para actualizar la ubicación de este inmueble");
    }

    property.setLatitude(request.latitude());
    property.setLongitude(request.longitude());
    property.setUpdatedAt(Instant.now());

    return mapToResponse(propertyRepository.save(property));
  }

  public PropertyResponse updateOperationType(String id, OperationType newType) {
    PropertyDocument prop =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inmueble no encontrado"));
    prop.setOperationType(newType);
    prop.setUpdatedAt(Instant.now());
    return mapToResponse(propertyRepository.save(prop));
  }

  /**
   * Mapea el documento a DTO incluyendo la generación de URLs temporales firmadas para imágenes.
   */
  public PropertyResponse mapToResponse(PropertyDocument doc) {
    List<String> urls = new ArrayList<>();

    if (doc.getImages() != null && !doc.getImages().isEmpty()) {
      urls =
          doc.getImages().stream()
              .map(img -> imageService.generateTemporaryImageUrl(img))
              .collect(Collectors.toList());
    } else if (doc.getImageUrls() != null) {
      urls = doc.getImageUrls();
    }

    String agentName = "No asignado";
    if (doc.getAssignedAgentId() != null && !doc.getAssignedAgentId().isBlank()) {
      try {
        IdentityClient.UserResponse agent = identityClient.findById(doc.getAssignedAgentId());
        if (agent != null) {
          agentName = agent.fullName();
        }
      } catch (Exception e) {
        log.warn(
            "Could not fetch agent name for ID: {}. Error: {}",
            doc.getAssignedAgentId(),
            e.getMessage());
        agentName = "Agente (" + doc.getAssignedAgentId() + ")";
      }
    }

    return new PropertyResponse(
        doc.getId(),
        doc.getTitle(),
        doc.getAddress(),
        doc.getZone() != null ? doc.getZone() : "No especificada",
        doc.getPrice(),
        doc.getType(),
        doc.getOperationType(),
        doc.getM2(),
        doc.getRooms(),
        doc.getStatus() != null ? doc.getStatus().name() : null,
        doc.getAssignedAgentId(),
        agentName,
        doc.getOwnerId(),
        urls,
        doc.getAssignmentHistory() != null ? doc.getAssignmentHistory() : new ArrayList<>(),
        doc.getPriceHistory() != null ? doc.getPriceHistory() : new ArrayList<>(),
        doc.getStatusHistory() != null ? doc.getStatusHistory() : new ArrayList<>(),
        doc.getAccessPolicy() != null ? doc.getAccessPolicy() : new HashSet<>(),
        doc.getLatitude(),
        doc.getLongitude(),
        doc.getMotivoRetiro(),
        doc.getDetalleRetiro());
  }

  /**
   * CORRECCIÓN CRÍTICA: Eliminación de imagen estrictamente por ID. Esto evita que el API Gateway
   * detecte caracteres maliciosos (//) en la URL de la petición.
   */
  public PropertyResponse deleteImage(String propertyId, String imageId) {
    PropertyDocument property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + propertyId));

    if (property.getImages() != null) {
      // Buscamos estrictamente por el ID interno generado en MongoDB (UUID)
      Optional<ImageMetadata> imgOpt =
          property.getImages().stream().filter(img -> img.getId().equals(imageId)).findFirst();

      if (imgOpt.isPresent()) {
        log.info("Eliminando imagen con ID: {} de la propiedad: {}", imageId, propertyId);
        imageService.deleteImage(propertyId, imageId);
      } else {
        log.error(
            "No se pudo borrar: la imagen con ID {} no pertenece a la propiedad {}",
            imageId,
            propertyId);
        throw new ResourceNotFoundException("La imagen solicitada no existe en este inmueble");
      }
    }

    return mapToResponse(propertyRepository.findById(propertyId).orElseThrow());
  }

  @Auditable(action = "PROPERTY_DELETE")
  public void deleteProperty(String id, String adminId) {
    PropertyDocument property =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inmueble no encontrado: " + id));

    property.setDeleted(true);
    property.setStatus(PropertyStatus.ELIMINADO); // Opcional: cambiar el status visual también
    property.setUpdatedAt(Instant.now());

    propertyRepository.save(property);

    log.info("Propiedad {} marcada como eliminada (lógico) por admin: {}", id, adminId);
  }

  @Auditable(action = "STATUS_CHANGE")
  public PropertyResponse updateStatus(
      String id, String newStatus, String currentUserId, List<String> roles, boolean isInternal) {
    PropertyDocument prop =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inmueble no encontrado"));

    PropertyStatus oldStatus = prop.getStatus();
    PropertyStatus targetStatus = PropertyStatus.valueOf(newStatus.toUpperCase());
    boolean isAdmin = roles.contains("ROLE_ADMIN");
    boolean isAssignedAgent = currentUserId.equals(prop.getAssignedAgentId());

    // --- Restriction: Business states must be driven by Operations ---
    if (!isInternal && !isAdmin) {
      if (targetStatus == PropertyStatus.VENDIDO
          || targetStatus == PropertyStatus.RESERVADO
          || targetStatus == PropertyStatus.EN_NEGOCIACION) {
        throw new ValidationException(
            "El estado "
                + targetStatus
                + " solo puede ser asignado a través del módulo de Operaciones.");
      }
    }

    // --- PA3: Validación de permisos y transiciones críticas ---
    if (!isAdmin && !isAssignedAgent && !isInternal) {
      throw new AccessDeniedException("No autorizado para modificar esta propiedad.");
    }

    // Bloquear cualquier reversión de un estado "VENDIDO" (Terminal y definitivo)
    if (oldStatus == PropertyStatus.VENDIDO) {
      throw new ValidationException(
          "Esta propiedad ha sido marcada como VENDIDA de forma definitiva. No es posible revertir su estado.");
    }

    // TRIGGER: Si se cambia a VENDIDO manualmente (por admin), crear registro de Operación
    if (targetStatus == PropertyStatus.VENDIDO && !isInternal) {
      triggerManualClosureOperation(prop, currentUserId, roles);
    }

    if (targetStatus == PropertyStatus.RETIRADO) {
      throw new ValidationException(
          "No se puede cambiar el estado a RETIRADO a través de este endpoint. Use el endpoint específico /retirar.");
    }

    // --- Registro de Historial (PA:2) ---
    if (prop.getStatusHistory() == null) {
      prop.setStatusHistory(new ArrayList<>());
    }

    prop.getStatusHistory()
        .add(
            StatusHistory.builder()
                .oldStatus(oldStatus.name())
                .newStatus(targetStatus.name())
                .changedAt(Instant.now())
                .changedBy(currentUserId)
                .build());

    prop.setStatus(targetStatus);
    prop.setUpdatedAt(Instant.now());

    return mapToResponse(propertyRepository.save(prop));
  }

  private void triggerManualClosureOperation(
      PropertyDocument prop, String currentUserId, List<String> roles) {
    try {
      String ownerName = "Desconocido";
      if (prop.getOwnerId() != null) {
        try {
          var res = identityClient.findById(prop.getOwnerId());
          ownerName = res.fullName();
        } catch (Exception e) {
          log.warn("Could not fetch owner name for manual closure: {}", e.getMessage());
        }
      }

      String agentName = "Administrador";
      String dept = "Gerencia";
      try {
        var res = identityClient.findById(currentUserId);
        agentName = res.fullName();
        dept = res.department();
      } catch (Exception e) {
        log.warn("Could not fetch agent name for manual closure: {}", e.getMessage());
      }

      var opRequest =
          com.inmobiliaria.property_service.client.OperationClient.CreateOperationRequest.builder()
              .propertyId(prop.getId())
              .propertyName(prop.getTitle())
              .propertyType(prop.getType())
              .operationType(prop.getOperationType().name())
              .finalPrice(prop.getPrice())
              .currency("USD")
              .clientId("ADMIN_MANUAL")
              .clientName("Cierre Directo por Admin")
              .agentId(currentUserId)
              .agentName(agentName)
              .department(dept)
              .ownerId(prop.getOwnerId())
              .ownerName(ownerName)
              .status("CLOSED")
              .closureDate(java.time.LocalDateTime.now())
              .notes("Operación generada automáticamente por cambio de estado manual a VENDIDO.")
              .build();

      operationClient.createOperation(opRequest, currentUserId, String.join(",", roles));
      log.info("Manual closure operation triggered for property {}", prop.getId());
    } catch (Exception e) {
      log.error("Failed to trigger manual closure operation: {}", e.getMessage());
      throw new ValidationException(
          "No se pudo registrar la operación de cierre obligatoria: " + e.getMessage());
    }
  }

  @Auditable(action = "PROPERTY_UPDATE")
  public PropertyResponse updateProperty(String id, PropertyRequest request, String adminId) {
    PropertyDocument property =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

    Double currentPrice = property.getPrice();

    property.setTitle(request.title());
    property.setAddress(request.address());
    property.setZone(request.zone());
    property.setType(request.type());
    property.setM2(request.m2());
    property.setRooms(request.rooms());
    property.setOperationType(request.operationType());

    if (request.ownerId() != null && !request.ownerId().isBlank()) {
      property.setOwnerId(request.ownerId());
    }

    property.setUpdatedAt(Instant.now());
    property.setCreatedBy(adminId);

    propertyRepository.save(property);

    if (request.price() != null && !request.price().equals(currentPrice)) {
      updatePrice(id, request.price(), adminId);
      log.info(
          "Price updated for property {} from {} to {} by admin {}",
          id,
          currentPrice,
          request.price(),
          adminId);
    }

    log.info("Property {} updated by admin {}", id, adminId);

    return mapToResponse(propertyRepository.findById(id).orElseThrow());
  }

  @Auditable(action = "PROPERTY_RETIRE")
  public PropertyResponse retireProperty(
      String id, RetirePropertyRequest request, String userId, List<String> roles) {
    // Reemplazar findDocumentById(id) por búsqueda directa
    PropertyDocument property =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

    boolean isAdmin = roles.contains("ROLE_ADMIN");
    boolean isAssignedAgent =
        property.getAssignedAgentId() != null && property.getAssignedAgentId().equals(userId);
    if (!isAdmin && !isAssignedAgent) {
      throw new AccessDeniedException("No tiene permisos para retirar este inmueble.");
    }

    // Validación de estado previo
    if (property.getStatus() == PropertyStatus.VENDIDO
        || property.getStatus() == PropertyStatus.ELIMINADO) {
      throw new ValidationException(
          "No se puede retirar un inmueble que ya está VENDIDO o ELIMINADO.");
    }
    if (property.getStatus() == PropertyStatus.RETIRADO) {
      throw new ValidationException("El inmueble ya se encuentra retirado.");
    }

    if (request.getMotivoRetiro() == RetirementReason.OTRO) {
      if (request.getDetalleRetiro() == null || request.getDetalleRetiro().isBlank()) {
        throw new ValidationException("Debe proporcionar un detalle cuando el motivo es 'Otro'.");
      }
    }

    // Registrar historial de estado
    if (property.getStatusHistory() == null) property.setStatusHistory(new ArrayList<>());
    property
        .getStatusHistory()
        .add(
            StatusHistory.builder()
                .oldStatus(property.getStatus().name())
                .newStatus(PropertyStatus.RETIRADO.name())
                .changedAt(Instant.now())
                .changedBy(userId)
                .build());

    // Actualizar campos
    property.setStatus(PropertyStatus.RETIRADO);
    property.setMotivoRetiro(request.getMotivoRetiro());
    property.setDetalleRetiro(request.getDetalleRetiro());
    property.setUpdatedAt(Instant.now());

    return mapToResponse(propertyRepository.save(property));
  }

  public void validateAvailabilityForAction(String id) {
    PropertyDocument prop =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inmueble no encontrado"));

    if (prop.getStatus() == PropertyStatus.VENDIDO
        || prop.getStatus() == PropertyStatus.RESERVADO) {
      throw new ValidationException(
          "El sistema no permite realizar esta acción: La propiedad ya está " + prop.getStatus());
    }
  }

  @Auditable(action = "PROPERTY_REINCORPORATE")
  public PropertyResponse reincorporateProperty(String id, String userId) {
    PropertyDocument prop1 =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inmueble no encontrado"));

    PropertyStatus currentStatus = prop1.getStatus();

    // Validación de estados previos permitidos (VENDIDO o RETIRADO)
    if (currentStatus != PropertyStatus.VENDIDO && currentStatus != PropertyStatus.RETIRADO) {
      throw new ValidationException(
          "Solo se pueden reincorporar inmuebles con estado VENDIDO o RETIRADO. Estado actual: "
              + currentStatus);
    }

    // Registrar en el historial de estados
    if (prop1.getStatusHistory() == null) {
      prop1.setStatusHistory(new ArrayList<>());
    }

    prop1
        .getStatusHistory()
        .add(
            StatusHistory.builder()
                .oldStatus(currentStatus.name())
                .newStatus(PropertyStatus.DISPONIBLE.name())
                .changedAt(Instant.now())
                .changedBy(userId)
                .build());

    // Actualizar estado principal
    prop1.setStatus(PropertyStatus.DISPONIBLE);
    prop1.setUpdatedAt(Instant.now());
    prop1.setMotivoRetiro(null);
    prop1.setDetalleRetiro(null);

    log.info("Inmueble {} reincorporado al inventario por usuario {}", id, userId);
    return mapToResponse(propertyRepository.save(prop1));
  }
}
