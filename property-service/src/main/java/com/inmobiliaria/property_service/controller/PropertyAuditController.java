package com.inmobiliaria.property_service.controller;

import com.inmobiliaria.property_service.domain.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/properties/audit")
@RequiredArgsConstructor
public class PropertyAuditController {

    private final MongoTemplate mongoTemplate;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> getLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String propertyId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        
        Query query = new Query();
        if (userId != null) query.addCriteria(Criteria.where("userId").is(userId));
        if (propertyId != null) query.addCriteria(Criteria.where("propertyId").is(propertyId));
        if (from != null && to != null) query.addCriteria(Criteria.where("timestamp").gte(from).lte(to));
        
        return mongoTemplate.find(query, AuditLog.class);
    }
}