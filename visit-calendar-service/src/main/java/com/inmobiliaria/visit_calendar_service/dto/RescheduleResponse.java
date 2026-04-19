package com.inmobiliaria.visit_calendar_service.dto;

import java.time.LocalDateTime;

import com.inmobiliaria.visit_calendar_service.model.Visit;
import com.inmobiliaria.visit_calendar_service.model.Visit.VisitStatus;

import lombok.Data;

/**
 * Response body for POST /visits/{id}/reschedule
 *
 * Returns the newly created visit along with a reference
 * to the original cancelled visit, so the frontend can render a link.
 *
 * PA: The new visit keeps the reference to the client, property,
 * and the original cancelled visit (originVisitId).
 */
@Data
public class RescheduleResponse {

    // ── New visit data ────────────────────────────────────────────────────
    private String newVisitId;
    private String propertyId;
    private String clientId;
    private String agentId;
    private LocalDateTime scheduledDateTime;
    private VisitStatus status;
    private String notes;
    private LocalDateTime createdAt;

    // ── Link back to the original cancelled visit ─────────────────────────
    /** ID of the original cancelled visit that was rescheduled */
    private String originVisitId;

    /** Convenience message shown in the UI */
    private String message;

    // ── Factory ───────────────────────────────────────────────────────────

    public static RescheduleResponse from(Visit newVisit, String message) {
        RescheduleResponse dto = new RescheduleResponse();
        dto.newVisitId = newVisit.getId();
        dto.propertyId = newVisit.getPropertyId();
        dto.clientId = newVisit.getClientId();
        dto.agentId = newVisit.getAgentId();
        dto.scheduledDateTime = newVisit.getDateTime();
        dto.status = newVisit.getStatus();
        dto.notes = newVisit.getNotes();
        dto.createdAt = newVisit.getCreatedAt();
        dto.originVisitId = newVisit.getOriginVisitId();
        dto.message = message;
        return dto;
    }
}
