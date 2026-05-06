package com.inmobiliaria.user_service.dto.request;

import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

public record SearchPreferencesRequest(
    List<String> preferredZones,
    @Min(0) Integer minRooms,
    @Min(0) Integer maxRooms,
    Double maxPrice,
    String preferredPropertyType) {
  @AssertTrue(message = "El número mínimo de cuartos no puede ser mayor al máximo")
  public boolean isValidRoomRange() {
    if (minRooms == null || maxRooms == null) return true;
    return minRooms <= maxRooms;
  }
}
