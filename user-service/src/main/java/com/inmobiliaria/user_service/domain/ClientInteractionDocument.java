package com.inmobiliaria.user_service.domain;

import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "client_interactions")
@CompoundIndex(name = "client_occurred_idx", def = "{'clientId': 1, 'occurredAt': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientInteractionDocument {

  @Id private String id;

  @Indexed private String clientId;

  @Indexed private String agentId;

  @Indexed private String propertyId;

  private String propertyName;

  private String agentName;

  private ClientInteractionType type;

  private Instant occurredAt;

  private String detail;

  private String subType;

  @Indexed(unique = true, sparse = true)
  private String referenceId;
}
