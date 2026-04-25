package com.inmobiliaria.visit_calendar_service.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inmobiliaria.visit_calendar_service.dto.response.ApiResponse;
import com.inmobiliaria.visit_calendar_service.dto.response.ResponseFactory;
import com.inmobiliaria.visit_calendar_service.model.Vehicle;
import com.inmobiliaria.visit_calendar_service.service.VehicleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/vehicles")
public class VehicleController {

  private final VehicleService vehicleService;
  private final ResponseFactory responseFactory;

  @GetMapping
  public ResponseEntity<ApiResponse<List<Vehicle>>> getVehicles(
      @RequestParam(required = false) Boolean available,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
          LocalTime time) {

    if (Boolean.TRUE.equals(available) && date != null && time != null) {
      LocalDateTime dateTime = LocalDateTime.of(date, time);
      List<Vehicle> availableVehicles = vehicleService.getAvailableVehicles(dateTime);
      return ResponseEntity.ok(
          responseFactory.success(
              "Vehículos disponibles obtenidos correctamente", availableVehicles));
    }

    List<Vehicle> vehicles = vehicleService.getAllVehicles();
    return ResponseEntity.ok(
        responseFactory.success("Vehículos obtenidos correctamente", vehicles));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<Vehicle>> getVehicleById(@PathVariable String id) {
    Vehicle vehicle = vehicleService.getVehicleById(id);
    return ResponseEntity.ok(responseFactory.success("Vehículo encontrado", vehicle));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<Vehicle>> createVehicle(@Valid @RequestBody Vehicle vehicle) {
    Vehicle created = vehicleService.createVehicle(vehicle);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(responseFactory.created("Vehículo creado exitosamente", created));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<Vehicle>> updateVehicle(
      @PathVariable String id, @Valid @RequestBody Vehicle vehicle) {
    Vehicle updated = vehicleService.updateVehicle(id, vehicle);
    return ResponseEntity.ok(responseFactory.success("Vehículo actualizado exitosamente", updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteVehicle(@PathVariable String id) {
    vehicleService.deleteVehicle(id);
    return ResponseEntity.noContent().build();
  }
}
