package com.inmobiliaria.property_service.controller;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.inmobiliaria.property_service.domain.OperationType;
import com.inmobiliaria.property_service.domain.StatusHistory;
import com.inmobiliaria.property_service.dto.request.*;
import com.inmobiliaria.property_service.dto.response.ApiResponse;
import com.inmobiliaria.property_service.dto.response.PropertyResponse;
import com.inmobiliaria.property_service.dto.response.ResponsableResponse;
import com.inmobiliaria.property_service.dto.response.ResponseFactory;
import com.inmobiliaria.property_service.service.PropertyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyController {

  private final PropertyService propertyService;
  private final ResponseFactory responseFactory;

  // --- READ OPERATIONS ---

  @GetMapping("/agent/{agentId}")
  public ResponseEntity<ApiResponse<List<PropertyResponse>>> findByAgent(
      @PathVariable String agentId) {
    List<PropertyResponse> data = propertyService.findByAgent(agentId);
    return ResponseEntity.ok(responseFactory.success("Properties found", data));
  }

  @PatchMapping("/{id}/agent-update")
  @PreAuthorize("hasRole('AGENT')")
  public ResponseEntity<ApiResponse<PropertyResponse>> updatePropertyAsAgent(
      @PathVariable String id,
      @Valid @RequestBody AgentPropertyUpdateRequest request,
      @RequestHeader("X-Auth-User-Id") String agentId) {
    PropertyResponse data = propertyService.updatePropertyAsAgent(id, request, agentId);
    return ResponseEntity.ok(responseFactory.success("Property updated successfully", data));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<PropertyResponse>>> findAll(
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String zone,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) OperationType operationType,
      @RequestParam(required = false) Double minPrice,
      @RequestParam(required = false) Double maxPrice,
      @RequestParam(required = false) String agentId,
      @RequestParam(required = false, defaultValue = "price") String sortBy,
      @RequestParam(required = false, defaultValue = "ASC") String sortOrder,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "9") int pageSize) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return ResponseEntity.ok(
          responseFactory.success("No properties found", Collections.emptyList()));
    }

    String currentUserId = (String) auth.getPrincipal();
    List<String> roles =
        auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    var result =
        propertyService.findWithFilters(
            title,
            type,
            zone,
            status,
            operationType,
            minPrice,
            maxPrice,
            agentId,
            currentUserId,
            roles,
            sortBy,
            sortOrder,
            page,
            pageSize);

    // Convert Map result to Page logic manually with safe casting
    Object dataObj = result.get("data");
    List<PropertyResponse> content = new ArrayList<>();
    if (dataObj instanceof List<?>) {
      for (Object item : (List<?>) dataObj) {
        if (item instanceof PropertyResponse) {
          content.add((PropertyResponse) item);
        }
      }
    }

    int total = 0;
    Object totalObj = result.get("totalElements");
    if (totalObj instanceof Number) {
      total = ((Number) totalObj).intValue();
    }

    return ResponseEntity.ok(
        responseFactory.paginated(
            "Properties retrieved successfully", content, page, pageSize, total));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PropertyResponse>> findById(@PathVariable String id) {
    PropertyResponse data = propertyService.findById(id);
    return ResponseEntity.ok(responseFactory.success("Property found", data));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<PropertyResponse>> updateProperty(
      @PathVariable String id,
      @Valid @RequestBody PropertyRequest request,
      @RequestHeader("X-Auth-User-Id") String adminId) {
    PropertyResponse data = propertyService.updateProperty(id, request, adminId);
    return ResponseEntity.ok(responseFactory.success("Property updated successfully", data));
  }

  // --- WRITE OPERATIONS (PROPERTY AGGREGATE) ---

  @PostMapping
  @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<PropertyResponse>> create(
      @Valid @RequestBody PropertyRequest request,
      @RequestHeader("X-Auth-User-Id") String agentId) {
    PropertyResponse data = propertyService.create(request, agentId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(responseFactory.created("Property created successfully", data));
  }

  @PatchMapping("/{id}/assign-owner")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<PropertyResponse>> assignOwner(
      @PathVariable String id,
      @RequestBody AssignOwnerRequest request,
      @RequestHeader("X-Auth-User-Id") String adminId) {
    PropertyResponse data = propertyService.assignOwner(id, request.ownerId(), adminId);
    return ResponseEntity.ok(responseFactory.success("Owner assigned successfully", data));
  }

  @PatchMapping("/{id}/price")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<PropertyResponse>> updatePrice(
      @PathVariable String id,
      @Valid @RequestBody UpdatePriceRequest request,
      @RequestHeader("X-Auth-User-Id") String adminId) {
    PropertyResponse data = propertyService.updatePrice(id, request.newPrice(), adminId);
    return ResponseEntity.ok(responseFactory.success("Price updated successfully", data));
  }

  @PatchMapping("/{id}/assign-agent")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<PropertyResponse>> assignAgent(
      @PathVariable String id,
      @Valid @RequestBody AssignAgentRequest request,
      @RequestHeader("X-Auth-User-Id") String adminId) {
    PropertyResponse data = propertyService.assignAgent(id, request, adminId);
    return ResponseEntity.ok(responseFactory.success("Agent assigned successfully", data));
  }

  @PatchMapping("/{id}/operation-type")
  @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
  public ResponseEntity<ApiResponse<PropertyResponse>> updateOperationType(
      @PathVariable String id, @Valid @RequestBody UpdateOperationTypeRequest request) {
    PropertyResponse data = propertyService.updateOperationType(id, request.operationType());
    return ResponseEntity.ok(responseFactory.success("Operation type updated successfully", data));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> delete(
      @PathVariable String id, @RequestHeader("X-Auth-User-Id") String adminId) {
    propertyService.deleteProperty(id, adminId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body(responseFactory.deleted("Property deleted successfully"));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<PropertyResponse>> updateStatus(
      @PathVariable String id,
      @Valid @RequestBody UpdateStatusRequest request,
      @RequestHeader("X-Auth-User-Id") String userId) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    List<String> roles =
        auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    PropertyResponse data = propertyService.updateStatus(id, request.status(), userId, roles);
    return ResponseEntity.ok(responseFactory.success("Status updated successfully", data));
  }

  @GetMapping("/{id}/status-history")
  @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
  public ResponseEntity<ApiResponse<List<StatusHistory>>> getStatusHistory(
      @PathVariable String id) {
    List<StatusHistory> data = propertyService.findById(id).statusHistory();
    return ResponseEntity.ok(responseFactory.success("Status history retrieved", data));
  }

  @GetMapping("/{id}/responsable")
  public ResponseEntity<ApiResponse<ResponsableResponse>> getResponsable(@PathVariable String id) {
    ResponsableResponse data = propertyService.getResponsable(id);
    return ResponseEntity.ok(responseFactory.success("Responsable retrieved", data));
  }

  @GetMapping("/owner/{ownerId}")
  public ResponseEntity<ApiResponse<List<PropertyResponse>>> findByOwner(
      @PathVariable String ownerId) {
    List<PropertyResponse> data = propertyService.findByOwner(ownerId);
    return ResponseEntity.ok(responseFactory.success("Properties found", data));
  }

  // ... (dentro de PropertyController)

@PatchMapping("/{id}/location")
@PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
public ResponseEntity<ApiResponse<PropertyResponse>> updateLocation(
        @PathVariable String id,
        @Valid @RequestBody UpdateLocationRequest request,
        @RequestHeader("X-Auth-User-Id") String userId) {
    
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    List<String> roles = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

    PropertyResponse data = propertyService.updateLocation(id, request, userId, roles);
    return ResponseEntity.ok(responseFactory.success("Ubicación geográfica actualizada correctamente", data));
}
}
