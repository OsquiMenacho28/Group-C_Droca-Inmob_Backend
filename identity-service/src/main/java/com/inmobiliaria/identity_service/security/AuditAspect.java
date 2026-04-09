package com.inmobiliaria.identity_service.security;

import com.inmobiliaria.identity_service.domain.AuditLog;
import com.inmobiliaria.identity_service.domain.UserDocument;
import com.inmobiliaria.identity_service.repository.AuditLogRepository;
import com.inmobiliaria.identity_service.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void auditAction(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String performedById = (auth != null) ? (String) auth.getPrincipal() : "SYSTEM";
            
            // Obtener información del usuario que ejecuta la acción
            String performedByEmail = "SYSTEM";
            String performedByName = "SYSTEM";
            
            if (!"SYSTEM".equals(performedById)) {
                Optional<UserDocument> performer = userRepository.findById(performedById);
                if (performer.isPresent()) {
                    performedByEmail = performer.get().getEmailNormalized();
                    performedByName = performer.get().getFullName();
                }
            }
            
            // Extraer el ID del usuario afectado (primer argumento del método o del resultado)
            String affectedUserId = extractAffectedUserId(joinPoint, result);
            String affectedUserEmail = null;
            String affectedUserName = null;
            
            if (affectedUserId != null && !"UNKNOWN".equals(affectedUserId)) {
                Optional<UserDocument> affected = userRepository.findById(affectedUserId);
                if (affected.isPresent()) {
                    affectedUserEmail = affected.get().getEmailNormalized();
                    affectedUserName = affected.get().getFullName();
                }
            }
            
            // Obtener IP address
            String ipAddress = getClientIp();
            
            // Construir detalles adicionales
            String details = buildDetails(joinPoint, auditable, result);
            
            // Crear y guardar log de auditoría
            AuditLog auditLog = AuditLog.builder()
                    .action(auditable.action())
                    .userId(affectedUserId)
                    .userEmail(affectedUserEmail)
                    .performedBy(performedById)
                    .performedByEmail(performedByEmail)
                    .performedByName(performedByName)
                    .timestamp(Instant.now())
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Audit record saved for action: {} by user: {}", auditable.action(), performedByEmail);
            
        } catch (Exception e) {
            log.error("Failed to generate audit log: {}", e.getMessage(), e);
        }
    }
    
    private String extractAffectedUserId(JoinPoint joinPoint, Object result) {
        // Intentar extraer del primer argumento (normalmente el ID)
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        
        // Si el resultado es UserResponse, extraer el ID
        if (result != null && result instanceof com.inmobiliaria.identity_service.dto.response.UserResponse) {
            return ((com.inmobiliaria.identity_service.dto.response.UserResponse) result).id();
        }
        
        return "UNKNOWN";
    }
    
    private String buildDetails(JoinPoint joinPoint, Auditable auditable, Object result) {
        StringBuilder details = new StringBuilder();
        details.append(auditable.description());
        
        if (!auditable.description().isEmpty() && !auditable.description().endsWith(".")) {
            details.append(". ");
        }
        
        // Añadir información adicional basada en la acción
        if (result != null && result instanceof com.inmobiliaria.identity_service.dto.response.UserResponse) {
            var userResp = (com.inmobiliaria.identity_service.dto.response.UserResponse) result;
            details.append(" User: ").append(userResp.email())
                   .append(" (").append(userResp.userType()).append(")");
        }
        
        return details.toString();
    }
    
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.debug("Could not get client IP: {}", e.getMessage());
        }
        return "UNKNOWN";
    }
}