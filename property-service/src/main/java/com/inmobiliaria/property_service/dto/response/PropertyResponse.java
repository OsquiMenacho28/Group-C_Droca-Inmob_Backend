package com.inmobiliaria.property_service.dto.response;

import java.util.List;
import java.util.Set;

import com.inmobiliaria.property_service.domain.AssignmentHistory;
import com.inmobiliaria.property_service.domain.OperationType;
import com.inmobiliaria.property_service.domain.PriceHistory;
import com.inmobiliaria.property_service.domain.StatusHistory;

public record PropertyResponse(
    String id,
    String title,
    String address,
    String zone,
    Double price,
    String type,
    OperationType operationType,
    Double m2,
    Integer rooms,
    String status,
    String assignedAgentId,
    String agentName,
    String ownerId,
    List<String> imageUrls,
    List<AssignmentHistory> assignmentHistory,
    List<PriceHistory> priceHistory,
    List<StatusHistory> statusHistory,
    Set<String> accessPolicy) {}
