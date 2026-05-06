// REEMPLAZAR el archivo completo:
package com.inmobiliaria.user_service.domain;

import java.util.List;

import org.springframework.data.annotation.TypeAlias;

import lombok.*;

@TypeAlias("interested_client")
@Getter
@Setter
@NoArgsConstructor
public class InterestedClientDocument extends PersonDocument {
  private String preferredContactMethod;
  private String budget;
  private List<String> interestedPropertyIds;

  // Campos nuevos de preferencias
  private List<String> preferredZones;
  private Integer minRooms;
  private Integer maxRooms;
  private Double maxPrice;
  private String preferredPropertyType;

  // Campos para baja lógica
  private boolean activo = true;
  private java.time.LocalDate lastActivityDate;
  private java.time.LocalDate fechaBaja;
  private String motivoBaja;
  private String preferredZone;
  private Integer preferredRooms;

  @Builder
  public InterestedClientDocument(
      String id,
      String authUserId,
      String firstName,
      String lastName,
      String fullName,
      java.time.LocalDate birthDate,
      String phone,
      String email,
      java.util.List<String> roleIds,
      boolean customRole,
      String assignedAgentId,
      String preferredContactMethod,
      String budget,
      List<String> interestedPropertyIds,
      String preferredZone,
      String preferredPropertyType,
      Integer preferredRooms,
      java.time.LocalDate lastActivityDate) {
    super(
        id,
        authUserId,
        firstName,
        lastName,
        fullName,
        birthDate,
        phone,
        email,
        PersonType.INTERESTED_CLIENT,
        roleIds,
        customRole,
        assignedAgentId);
    this.preferredContactMethod = preferredContactMethod;
    this.budget = budget;
    this.interestedPropertyIds = interestedPropertyIds;
    this.preferredZone = preferredZone;
    this.preferredPropertyType = preferredPropertyType;
    this.preferredRooms = preferredRooms;
    this.lastActivityDate = lastActivityDate;
  }
}
