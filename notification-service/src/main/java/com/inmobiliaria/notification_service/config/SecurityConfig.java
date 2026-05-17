package com.inmobiliaria.notification_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth
                    // Endpoint interno: requiere cabecera X-Service-Name
                    .requestMatchers("/notifications/in-app")
                    .access(new HasServiceNameAuthorizationManager())
                    // Endpoints de usuario autenticado
                    .requestMatchers("/notifications/in-app/**")
                    .authenticated()
                    // Resto públicos (credential email, etc.)
                    .anyRequest()
                    .permitAll());
    return http.build();
  }
}
