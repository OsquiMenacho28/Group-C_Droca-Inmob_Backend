package com.inmobiliaria.property_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.inmobiliaria.property_service.security.PropertyPermissionInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final PropertyPermissionInterceptor propertyPermissionInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(propertyPermissionInterceptor)
        .addPathPatterns("/properties/*/images/upload");
  }
}
