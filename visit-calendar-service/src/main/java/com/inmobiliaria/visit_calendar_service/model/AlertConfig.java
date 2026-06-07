// backend/visit-calendar-service/src/main/java/.../model/AlertConfig.java
package com.inmobiliaria.visit_calendar_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "alert_config")
public class AlertConfig {
  @Id private String id; // único, p.ej. "DEFAULT"
  private int anticipationHours; // ej. 2 horas
  private String channel; // "IN_APP", "EMAIL", "BOTH"
  private boolean enabled = true;
}
