package com.inmobiliaria.access_control_service.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.inmobiliaria.access_control_service.dto.request.CreateRoleRequest;
import com.inmobiliaria.access_control_service.dto.request.UpdateRoleRequest;
import com.inmobiliaria.access_control_service.dto.response.ApiResponse;
import com.inmobiliaria.access_control_service.dto.response.ResponseFactory;
import com.inmobiliaria.access_control_service.dto.response.RoleResponse;
import com.inmobiliaria.access_control_service.service.RoleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

  private final RoleService roleService;
  private final ResponseFactory responseFactory;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<RoleResponse>>> findAll(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int pageSize) {
    Page<RoleResponse> roles = roleService.findAll(PageRequest.of(page, pageSize));
    return ResponseEntity.ok(responseFactory.paginated("Roles retrieved successfully", roles));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<RoleResponse>> findById(@PathVariable String id) {
    RoleResponse role = roleService.findById(id);
    return ResponseEntity.ok(responseFactory.success("Role retrieved successfully", role));
  }

  @GetMapping("/validate")
  public ResponseEntity<ApiResponse<Boolean>> validateRoleIds(@RequestParam List<String> ids) {
    boolean allValid =
        ids.stream()
            .allMatch(
                id -> {
                  try {
                    roleService.findById(id);
                    return true;
                  } catch (Exception e) {
                    return false;
                  }
                });
    return ResponseEntity.ok(responseFactory.success("Role validation completed", allValid));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<RoleResponse>> create(
      @Valid @RequestBody CreateRoleRequest request) {
    RoleResponse role = roleService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(responseFactory.created("Role created successfully", role));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<RoleResponse>> update(
      @PathVariable String id, @Valid @RequestBody UpdateRoleRequest request) {
    RoleResponse role = roleService.update(id, request);
    return ResponseEntity.ok(responseFactory.success("Role updated successfully", role));
  }
}
