package com.inmobiliaria.user_service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.inmobiliaria.user_service.config.FeignConfig;

import lombok.Builder;

@FeignClient(name = "identity-service", configuration = FeignConfig.class)
public interface IdentityClient {

  @GetMapping("/users/{id}")
  UserResponse findById(@PathVariable("id") String id);

  @Builder
  record UserResponse(
      String id, String email, String userType, String status, List<String> primaryRoleIds) {}
}
