package com.inmobiliaria.property_service.security;

import com.inmobiliaria.property_service.domain.PropertyDocument;
import com.inmobiliaria.property_service.dto.response.PropertyResponse;
import com.inmobiliaria.property_service.domain.AuditLog;
import com.inmobiliaria.property_service.repository.AuditLogRepository;
import com.inmobiliaria.property_service.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;

// ... existing imports

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final PropertyRepository propertyRepository;

    @Around("@annotation(auditable)")
    public Object auditPropertyChange(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String action = auditable.action();
        String propertyId = null;
        String previousValue = "N/A";

        // 1. Capture previous state
        if (!action.equals("PROPERTY_CREATE") && args.length > 0 && args[0] instanceof String) {
            propertyId = (String) args[0];
            PropertyDocument prop = propertyRepository.findById(propertyId).orElse(null);
            if (prop != null) {
                previousValue = switch (action) {
                    case "STATUS_CHANGE" -> prop.getStatus();
                    case "PRICE_UPDATE" -> String.valueOf(prop.getPrice());
                    case "AGENT_ASSIGN" -> prop.getAssignedAgentId();
                    case "OWNER_ASSIGN" -> prop.getOwnerId();
                    // IMPROVEMENT: Capture Title and Type instead of hardcoded string
                    case "PROPERTY_UPDATE" -> String.format("%s (%s)", prop.getTitle(), prop.getType());
                    default -> prop.getStatus();
                };
            }
        }

        // 2. Execute method
        Object result = joinPoint.proceed();

        // 3. Capture new state
        String newValue = "N/A";
        if (action.equals("PROPERTY_CREATE") && result instanceof PropertyResponse res) {
            propertyId = res.id();
            newValue = res.title(); 
            previousValue = "NEW_PROPERTY";
        } else if (propertyId != null) {
            PropertyDocument updated = propertyRepository.findById(propertyId).orElse(null);
            if (updated != null) {
                newValue = switch (action) {
                    case "STATUS_CHANGE" -> updated.getStatus();
                    case "PRICE_UPDATE" -> String.valueOf(updated.getPrice());
                    case "AGENT_ASSIGN" -> updated.getAssignedAgentId();
                    case "OWNER_ASSIGN" -> updated.getOwnerId();
                    case "PROPERTY_DELETE" -> "DELETED";
                    // IMPROVEMENT: Capture Title and Type
                    case "PROPERTY_UPDATE" -> String.format("%s (%s)", updated.getTitle(), updated.getType());
                    default -> "Modified";
                };
            }
        }

        // 4. Save audit log
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .action(action)
                .propertyId(propertyId)
                .previousValue(previousValue)
                .newValue(newValue)
                .timestamp(Instant.now())
                .build();

        auditLogRepository.save(log);
        return result;
    }
}