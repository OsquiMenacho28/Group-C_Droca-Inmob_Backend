package com.inmobiliaria.operation_service.controller;

import com.inmobiliaria.operation_service.dto.response.ApiResponse;
import com.inmobiliaria.operation_service.dto.response.ResponseFactory;
import com.inmobiliaria.operation_service.service.DashboardService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operations/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;
  private final ResponseFactory responseFactory;

  @GetMapping("/resumen")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<DashboardService.DashboardSummaryResponse>> getResumen() {
    return ResponseEntity.ok(
        responseFactory.success("Resumen obtenido exitosamente", dashboardService.getResumen()));
  }

  @GetMapping("/distribucion-estados")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Map<String, Long>>> getDistribucionEstados() {
    return ResponseEntity.ok(
        responseFactory.success(
            "Distribución obtenida exitosamente", dashboardService.getDistribucionEstados()));
  }
}
