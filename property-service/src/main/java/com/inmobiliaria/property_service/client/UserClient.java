package com.inmobiliaria.property_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.inmobiliaria.property_service.config.FeignConfig;

@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserClient {
  @GetMapping("/persons/{id}")
  UserPreferenceResponse getPersonPreferences(@PathVariable("id") String id);

  record UserPreferenceResponse(
      String id,
      java.util.List<String> preferredZones,
      Integer minRooms,
      Integer maxRooms,
      Double maxPrice,
      String preferredPropertyType) {}
}
