package com.inmobiliaria.operation_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "operations")
public class Operation {
    @Id
    private String id;
    private String propertyId;
    private String propertyName;
    private String clientId;
    private String clientName;
    private String agentId;
    private String agentName;
    private String status;
    private LocalDateTime createdAt;
}