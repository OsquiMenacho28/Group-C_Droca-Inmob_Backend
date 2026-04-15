package com.inmobiliaria.operation_service.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "operations")
public class Operation {
  @Id private String id;
  private String propertyId;
  private String propertyName;
  private String clientId;
  private String clientName;
  private String agentId;
  private String agentName;
  private String status;
  private LocalDateTime createdAt;
}
