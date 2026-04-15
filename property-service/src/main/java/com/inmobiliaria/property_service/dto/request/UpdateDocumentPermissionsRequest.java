package com.inmobiliaria.property_service.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDocumentPermissionsRequest {

  @NotBlank(message = "Document ID is required")
  private String documentId;

  @NotNull(message = "Access policy is required")
  private Set<String> accessPolicy; // Roles (ROLE_ADMIN, ROLE_AGENT) or user IDs
}
