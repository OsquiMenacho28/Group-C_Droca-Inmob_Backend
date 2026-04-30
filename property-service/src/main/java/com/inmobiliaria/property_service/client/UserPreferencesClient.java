package com.inmobiliaria.property_service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.inmobiliaria.property_service.config.FeignConfig;

@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserPreferencesClient {

  @GetMapping("/persons/{id}")
  UserPrefResponse getPreferences(@PathVariable("id") String id);

  record UserPrefResponse(
      String id,
      List<String> preferredZones,
      Integer minRooms,
      Integer maxRooms,
      Double maxPrice,
      String preferredPropertyType) {}
}
