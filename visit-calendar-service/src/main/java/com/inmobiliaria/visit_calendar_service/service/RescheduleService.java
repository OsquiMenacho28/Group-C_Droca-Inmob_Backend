package com.inmobiliaria.visit_calendar_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.inmobiliaria.visit_calendar_service.dto.RescheduleRequest;
import com.inmobiliaria.visit_calendar_service.dto.RescheduleResponse;
import com.inmobiliaria.visit_calendar_service.model.ReschedulingHistory;
import com.inmobiliaria.visit_calendar_service.model.Visit;
import com.inmobiliaria.visit_calendar_service.model.Visit.VisitStatus;
import com.inmobiliaria.visit_calendar_service.repository.VisitRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Business logic for rescheduling a cancelled visit.
 *
 * TD: Implement RescheduleService with full business logic and 409 guard.
 *
 * Flow:
 * 1. Load the original visit — 404 if not found.
 * 2. Guard: visit must be CANCELLED — 409 if not (PA3).
 * 3. Availability check: agent and property must be free in a 1-hour window.
 * 4. Create a new SCHEDULED visit copying client/property/agent from the original.
 * 5. Set originVisitId on the new visit to link it back (PA2).
 * 6. Append a ReschedulingHistory to the original visit's history.
 * 7. Save both documents and return the response.
 */
@Slf4j
@Service
public class RescheduleService {

        /**
         * Buffer around the proposed datetime used to detect scheduling conflicts.
         * A visit blocks 60 minutes before and after its scheduled time.
         */
        private static final long AVAILABILITY_BUFFER_MINUTES = 60L;

        private final VisitRepository visitRepository;

        public RescheduleService(VisitRepository visitRepository) {
                this.visitRepository = visitRepository;
        }

        // ─────────────────────────────────────────────────────────────────────────
        // RESCHEDULE — main entry point
        // POST /visits/{id}/reschedule
        // ─────────────────────────────────────────────────────────────────────────

        /**
         * Creates a new SCHEDULED visit rescheduled from a CANCELLED one.
         *
         * @param originalVisitId ID of the cancelled visit to reschedule
         * @param agentId Authenticated agent performing the action (from JWT)
         * @param request New datetime and optional notes
         * @return RescheduleResponse with the new visit data and originVisitId
         *
         * @throws ResponseStatusException 404 if the visit does not exist
         * @throws ResponseStatusException 409 if the visit is not in CANCELLED status (PA3)
         * @throws ResponseStatusException 422 if the agent or property is unavailable
         */
        public RescheduleResponse reschedule(String originalVisitId,
                        String agentId,
                        RescheduleRequest request) {

                // 1. Load original visit
                Visit original = visitRepository.findById(originalVisitId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Visit not found: " + originalVisitId));

                // 2. Status guard — PA3: cannot reschedule a REALIZADA or SCHEDULED visit
                if (original.getStatus() != VisitStatus.CANCELLED) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Only cancelled visits can be rescheduled. " +
                                                        "Current status: "
                                                        + original.getStatus().name());
                }

                // 3. Availability check — agent
                validateAgentAvailability(original.getAgentId(), request.getNewDateTime(), null);

                // 4. Availability check — property
                validatePropertyAvailability(original.getPropertyId(), request.getNewDateTime(),
                                null);

                // 5. Build the new visit (PA2: copy client, property, agent — add originVisitId)
                Visit newVisit = buildNewVisit(original, request, agentId);
                newVisit = visitRepository.save(newVisit);
                log.info("[RescheduleService] New visit created: id='{}', origin='{}'",
                                newVisit.getId(), originalVisitId);

                // 6. Append rescheduling record to the original visit's history
                ReschedulingHistory record = ReschedulingHistory.builder()
                                .newVisitId(newVisit.getId())
                                .previousDateTime(original.getDateTime())
                                .newDateTime(request.getNewDateTime())
                                .rescheduledByAgentId(agentId)
                                .rescheduledAt(LocalDateTime.now())
                                .build();

                if (original.getReschedulingHistory() == null) {
                        original.setReschedulingHistory(new java.util.ArrayList<>());
                }
                original.getReschedulingHistory().add(record);
                visitRepository.save(original);
                log.info("[RescheduleService] Rescheduling record appended to original visit '{}'",
                                originalVisitId);

                return RescheduleResponse.from(newVisit,
                                "Visit rescheduled successfully. New visit ID: "
                                                + newVisit.getId());
        }

        // ─────────────────────────────────────────────────────────────────────────
        // GET RESCHEDULED VISITS FROM ORIGINAL
        // ─────────────────────────────────────────────────────────────────────────

        /**
         * Returns all visits that were created by rescheduling the given original visit.
         * Used by the frontend to render the "View rescheduled visit" link.
         *
         * @param originVisitId The ID of the original cancelled visit
         */
        public List<Visit> getRescheduledVisits(String originVisitId) {
                return visitRepository.findByOriginVisitId(originVisitId);
        }

        // ─────────────────────────────────────────────────────────────────────────
        // Private helpers
        // ─────────────────────────────────────────────────────────────────────────

        /**
         * Validates that the agent has no SCHEDULED visit within the conflict window.
         * Throws 422 if unavailable.
         *
         * @param agentId Agent to check
         * @param dateTime Proposed datetime
         * @param excludeId Visit ID to exclude from the check (null = no exclusion)
         */
        private void validateAgentAvailability(String agentId,
                        LocalDateTime dateTime,
                        String excludeId) {
                LocalDateTime windowStart = dateTime.minusMinutes(AVAILABILITY_BUFFER_MINUTES);
                LocalDateTime windowEnd = dateTime.plusMinutes(AVAILABILITY_BUFFER_MINUTES);

                boolean conflict = visitRepository
                                .existsByAgentIdAndDateTimeBetweenAndStatus(
                                                agentId, windowStart, windowEnd,
                                                VisitStatus.SCHEDULED);

                if (conflict) {
                        throw new ResponseStatusException(
                                        HttpStatus.UNPROCESSABLE_ENTITY,
                                        "The agent already has a scheduled visit within 1 hour of the proposed time. "
                                                        +
                                                        "Please choose a different time slot.");
                }
        }

        /**
         * Validates that the property has no SCHEDULED visit within the conflict window.
         * Throws 422 if unavailable.
         */
        private void validatePropertyAvailability(String propertyId,
                        LocalDateTime dateTime,
                        String excludeId) {
                LocalDateTime windowStart = dateTime.minusMinutes(AVAILABILITY_BUFFER_MINUTES);
                LocalDateTime windowEnd = dateTime.plusMinutes(AVAILABILITY_BUFFER_MINUTES);

                boolean conflict = visitRepository
                                .existsByPropertyIdAndDateTimeBetweenAndStatus(
                                                propertyId, windowStart, windowEnd,
                                                VisitStatus.SCHEDULED);

                if (conflict) {
                        throw new ResponseStatusException(
                                        HttpStatus.UNPROCESSABLE_ENTITY,
                                        "The property already has a scheduled visit within 1 hour of the proposed time. "
                                                        +
                                                        "Please choose a different time slot.");
                }
        }

        /**
         * Builds the new Visit entity from the original, applying the new datetime.
         * Preserves: propertyId, clientId, agentId from the original (PA2).
         * Sets: originVisitId to link back to the original (PA2).
         */
        private Visit buildNewVisit(Visit original, RescheduleRequest request, String agentId) {
                Visit newVisit = new Visit();
                newVisit.setPropertyId(original.getPropertyId());
                newVisit.setClientId(original.getClientId());
                newVisit.setAgentId(original.getAgentId());
                newVisit.setDateTime(request.getNewDateTime());
                newVisit.setStatus(VisitStatus.SCHEDULED);
                newVisit.setNotes(request.getNotes() != null
                                ? request.getNotes()
                                : original.getNotes());
                newVisit.setOriginVisitId(original.getId());
                newVisit.setCreatedAt(LocalDateTime.now());
                newVisit.setReschedulingHistory(new java.util.ArrayList<>());
                return newVisit;
        }
}
