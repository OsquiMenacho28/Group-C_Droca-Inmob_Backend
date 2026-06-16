package com.inmobiliaria.user_service.service;

import com.inmobiliaria.user_service.client.PropertyClient;
import com.inmobiliaria.user_service.client.VisitRequestClient;
import com.inmobiliaria.user_service.domain.ClientInteractionDocument;
import com.inmobiliaria.user_service.domain.ClientInteractionType;
import com.inmobiliaria.user_service.domain.FavoriteHistoryDocument;
import com.inmobiliaria.user_service.domain.PersonDocument;
import com.inmobiliaria.user_service.dto.request.RecordClientInteractionRequest;
import com.inmobiliaria.user_service.dto.response.ClientInteractionResponse;
import com.inmobiliaria.user_service.exception.AccessDeniedException;
import com.inmobiliaria.user_service.exception.ResourceNotFoundException;
import com.inmobiliaria.user_service.repository.ClientInteractionRepository;
import com.inmobiliaria.user_service.repository.FavoriteHistoryRepository;
import com.inmobiliaria.user_service.repository.PersonRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientInteractionService {

  private final ClientInteractionRepository interactionRepository;
  private final FavoriteHistoryRepository favoriteHistoryRepository;
  private final PersonRepository personRepository;
  private final MongoTemplate mongoTemplate;
  private final PropertyClient propertyClient;
  private final VisitRequestClient visitRequestClient;

  public ClientInteractionResponse recordInteraction(RecordClientInteractionRequest request) {
    if (request.referenceId() != null
        && !request.referenceId().isBlank()
        && interactionRepository.existsByReferenceId(request.referenceId())) {
      return interactionRepository
          .findByReferenceId(request.referenceId())
          .map(this::toResponse)
          .orElseThrow();
    }

    Instant occurredAt = request.occurredAt() != null ? request.occurredAt() : Instant.now();

    ClientInteractionDocument saved =
        interactionRepository.save(
            ClientInteractionDocument.builder()
                .clientId(request.clientId())
                .agentId(request.agentId())
                .agentName(request.agentName())
                .propertyId(request.propertyId())
                .propertyName(request.propertyName())
                .type(request.type())
                .occurredAt(occurredAt)
                .detail(request.detail())
                .subType(request.subType())
                .referenceId(request.referenceId())
                .build());

    log.info(
        "Recorded {} interaction for client {} property {}",
        request.type(),
        request.clientId(),
        request.propertyId());

    return toResponse(saved);
  }

  public List<ClientInteractionResponse> getClientInteractions(
      String clientAuthUserId,
      String requesterId,
      ClientInteractionType type,
      Instant from,
      Instant to) {
    validateAccess(clientAuthUserId, requesterId);
    syncLegacyInteractions(clientAuthUserId);

    Query query = new Query(Criteria.where("clientId").is(clientAuthUserId));

    if (type != null) {
      query.addCriteria(Criteria.where("type").is(type));
    }
    if (from != null) {
      query.addCriteria(Criteria.where("occurredAt").gte(from));
    }
    if (to != null) {
      query.addCriteria(Criteria.where("occurredAt").lte(to));
    }

    query.with(Sort.by(Sort.Direction.DESC, "occurredAt"));

    return mongoTemplate.find(query, ClientInteractionDocument.class).stream()
        .map(this::toResponse)
        .toList();
  }

  public void recordFavoriteInteraction(
      String clientAuthUserId, String propertyId, Instant occurredAt) {
    PersonDocument client =
        personRepository
            .findByAuthUserId(clientAuthUserId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Client not found with authUserId: " + clientAuthUserId));

    String agentId = client.getAssignedAgentId();
    String propertyName = propertyId;

    try {
      PropertyClient.PropertyResponse property = propertyClient.getProperty(propertyId);
      if (property.title() != null) {
        propertyName = property.title();
      }
    } catch (Exception e) {
      log.warn("Could not resolve property title for {}: {}", propertyId, e.getMessage());
    }

    if (agentId == null || agentId.isBlank()) {
      log.warn(
          "Skipping favorite interaction for client {} without assigned agent", clientAuthUserId);
      return;
    }

    Instant when = occurredAt != null ? occurredAt : Instant.now();
    String referenceId = "legacy-favorite-" + clientAuthUserId + "-" + propertyId + "-" + when;

    recordInteraction(
        new RecordClientInteractionRequest(
            clientAuthUserId,
            agentId,
            propertyId,
            propertyName,
            null,
            ClientInteractionType.FAVORITO,
            when,
            "ADDED",
            "ADDED",
            referenceId));
  }

  private void syncLegacyInteractions(String clientAuthUserId) {
    syncFavoriteHistory(clientAuthUserId);
    syncVisitRequests(clientAuthUserId);
  }

  private void syncFavoriteHistory(String clientAuthUserId) {
    List<FavoriteHistoryDocument> history =
        favoriteHistoryRepository.findByAuthUserIdOrderByTimestampDesc(
            clientAuthUserId, PageRequest.of(0, 200));

    for (FavoriteHistoryDocument entry : history) {
      if (!"ADDED".equalsIgnoreCase(entry.getAction())) {
        continue;
      }

      String referenceId =
          "legacy-favorite-"
              + clientAuthUserId
              + "-"
              + entry.getPropertyId()
              + "-"
              + entry.getTimestamp();

      if (interactionRepository.existsByReferenceId(referenceId)) {
        continue;
      }

      try {
        PersonDocument client =
            personRepository
                .findByAuthUserId(clientAuthUserId)
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "Client not found with authUserId: " + clientAuthUserId));

        String agentId = client.getAssignedAgentId();
        if (agentId == null || agentId.isBlank()) {
          continue;
        }

        String propertyName = entry.getPropertyId();
        try {
          PropertyClient.PropertyResponse property =
              propertyClient.getProperty(entry.getPropertyId());
          if (property.title() != null) {
            propertyName = property.title();
          }
        } catch (Exception ignored) {
          // keep propertyId as fallback
        }

        recordInteraction(
            new RecordClientInteractionRequest(
                clientAuthUserId,
                agentId,
                entry.getPropertyId(),
                propertyName,
                null,
                ClientInteractionType.FAVORITO,
                entry.getTimestamp(),
                entry.getAction(),
                entry.getAction(),
                referenceId));
      } catch (Exception e) {
        log.warn("Failed to sync favorite history {}: {}", referenceId, e.getMessage());
      }
    }
  }

  private void syncVisitRequests(String clientAuthUserId) {
    try {
      List<VisitRequestClient.VisitRequestSummary> visits =
          visitRequestClient.getClientRequests(clientAuthUserId);
      if (visits == null || visits.isEmpty()) {
        return;
      }

      for (VisitRequestClient.VisitRequestSummary visit : visits) {
        String referenceId = "visit-request-" + visit.id();
        if (interactionRepository.existsByReferenceId(referenceId)) {
          continue;
        }

        recordInteraction(
            new RecordClientInteractionRequest(
                visit.clientId(),
                visit.agentId(),
                visit.propertyId(),
                visit.propertyName(),
                visit.agentName(),
                ClientInteractionType.VISITA,
                visit.createdAt() != null ? visit.createdAt() : Instant.now(),
                visit.message(),
                visit.status(),
                referenceId));
      }
    } catch (Exception e) {
      log.warn("Could not sync visit requests for client {}: {}", clientAuthUserId, e.getMessage());
    }
  }

  private void validateAccess(String clientAuthUserId, String requesterId) {
    if (requesterId == null || requesterId.isBlank()) {
      throw new AccessDeniedException("Authenticated user is required");
    }

    if (hasAnyRole("ADMIN")) {
      return;
    }

    if (clientAuthUserId.equals(requesterId)) {
      return;
    }

    PersonDocument client =
        personRepository
            .findByAuthUserId(clientAuthUserId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Client not found with authUserId: " + clientAuthUserId));

    if (hasAnyRole("AGENT", "EMPLOYEE")
        && client.getAssignedAgentId() != null
        && client.getAssignedAgentId().equals(requesterId)) {
      return;
    }

    throw new AccessDeniedException("You can only view interactions for clients assigned to you.");
  }

  private boolean hasAnyRole(String... roles) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }

    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    for (String role : roles) {
      String normalized = "ROLE_" + role.toUpperCase();
      if (authorities.stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase(normalized))) {
        return true;
      }
    }
    return false;
  }

  public static Instant parseDateStart(String date) {
    return LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC);
  }

  public static Instant parseDateEnd(String date) {
    return LocalDate.parse(date).atTime(23, 59, 59, 999_000_000).toInstant(ZoneOffset.UTC);
  }

  private ClientInteractionResponse toResponse(ClientInteractionDocument doc) {
    return new ClientInteractionResponse(
        doc.getId(),
        doc.getClientId(),
        doc.getAgentId(),
        doc.getAgentName(),
        doc.getPropertyId(),
        doc.getPropertyName(),
        doc.getType(),
        doc.getOccurredAt(),
        doc.getDetail(),
        doc.getSubType());
  }
}
