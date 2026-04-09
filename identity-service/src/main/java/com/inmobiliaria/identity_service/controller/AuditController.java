package com.inmobiliaria.identity_service.controller;

import com.inmobiliaria.identity_service.domain.AuditLog;
import com.inmobiliaria.identity_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {
    
    private final AuditLogRepository auditLogRepository;
    
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }
}