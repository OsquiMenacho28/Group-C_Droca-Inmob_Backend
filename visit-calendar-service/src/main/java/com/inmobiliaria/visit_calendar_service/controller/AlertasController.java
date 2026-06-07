package com.inmobiliaria.visit_calendar_service.controller;

import com.inmobiliaria.visit_calendar_service.dto.response.ApiResponse;
import com.inmobiliaria.visit_calendar_service.dto.response.ResponseFactory;
import com.inmobiliaria.visit_calendar_service.model.AlertConfig;
import com.inmobiliaria.visit_calendar_service.model.Visit;
import com.inmobiliaria.visit_calendar_service.service.AlertConfigService;
import com.inmobiliaria.visit_calendar_service.service.UpcomingVisitsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/alertas")
public class AlertasController {

  private final UpcomingVisitsService upcomingVisitsService;
  private final AlertConfigService alertConfigService; // ← Agregado
  private final ResponseFactory responseFactory;

  @GetMapping("/visitas-proximas")
  public ResponseEntity<ApiResponse<List<Visit>>> getUpcomingVisits(
      @RequestHeader("X-Auth-User-Id") String userId,
      @RequestParam(required = false) Integer hours) {
    List<Visit> visits = upcomingVisitsService.getUpcomingVisitsForUser(userId, hours);
    return ResponseEntity.ok(responseFactory.success("Próximas visitas obtenidas", visits));
  }

  @PutMapping("/admin/configuracion-alertas")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<AlertConfig>> updateAlertConfig(
      @RequestParam int anticipationHours, @RequestParam String channel) {
    AlertConfig updated = alertConfigService.updateConfig(anticipationHours, channel);
    return ResponseEntity.ok(responseFactory.success("Configuración actualizada", updated));
  }
}
