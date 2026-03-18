package com.inmobiliaria.access_control_service.controller;

import com.inmobiliaria.access_control_service.dto.request.CreateRoleRequest;
import com.inmobiliaria.access_control_service.dto.request.UpdateRoleRequest;
import com.inmobiliaria.access_control_service.dto.response.RoleResponse;
import com.inmobiliaria.access_control_service.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<RoleResponse> findAll() {
        return roleService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoleResponse findById(@PathVariable String id) {
        return roleService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RoleResponse create(@Valid @RequestBody CreateRoleRequest request) {
        return roleService.create(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoleResponse update(@PathVariable String id, @Valid @RequestBody UpdateRoleRequest request) {
        return roleService.update(id, request);
    }
}