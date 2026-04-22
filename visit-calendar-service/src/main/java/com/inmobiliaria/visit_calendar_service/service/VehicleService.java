package com.inmobiliaria.visit_calendar_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.inmobiliaria.visit_calendar_service.exception.ResourceNotFoundException;
import com.inmobiliaria.visit_calendar_service.model.CalendarEvent;
import com.inmobiliaria.visit_calendar_service.model.Vehicle;
import com.inmobiliaria.visit_calendar_service.model.Visit;
import com.inmobiliaria.visit_calendar_service.repository.CalendarEventRepository;
import com.inmobiliaria.visit_calendar_service.repository.VehicleRepository;
import com.inmobiliaria.visit_calendar_service.repository.VisitRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio agnóstico para la gestión de flota y asignación de vehículos. Implementa un patrón de
 * agregador para asegurar la consistencia de datos entre diferentes tipos de entidades
 * (CalendarEvent y Visit).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

  private final VehicleRepository vehicleRepository;
  private final CalendarEventRepository calendarEventRepository;
  private final VisitRepository visitRepository;

  // ─── Gestión de Vehículos (CRUD) ─────────────────────────────────────────

  public List<Vehicle> getAllVehicles() {
    return vehicleRepository.findAll();
  }

  public Vehicle createVehicle(Vehicle vehicle) {
    return vehicleRepository.save(vehicle);
  }

  public Vehicle updateVehicle(String id, Vehicle vehicle) {
    Vehicle existing =
        vehicleRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado: " + id));
    existing.setLicensePlate(vehicle.getLicensePlate());
    existing.setBrand(vehicle.getBrand());
    existing.setModel(vehicle.getModel());
    existing.setPassengerCapacity(vehicle.getPassengerCapacity());
    existing.setStatus(vehicle.getStatus());
    return vehicleRepository.save(existing);
  }

  public void deleteVehicle(String id) {
    vehicleRepository.deleteById(id);
  }

  // ─── Lógica Agnóstica de Disponibilidad ──────────────────────────────────

  /**
   * Verifica la disponibilidad de un vehículo consultando TODAS las fuentes de ocupación. Esto
   * asegura que un vehículo no sea reservado doblemente si existe en CalendarEvent o en Visit.
   */
  public void checkVehicleAvailability(
      String vehicleId, LocalDateTime start, LocalDateTime end, String excludeId) {
    Vehicle vehicle =
        vehicleRepository
            .findById(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado"));

    if (vehicle.getStatus() == Vehicle.VehicleStatus.MAINTENANCE) {
      throw new RuntimeException("El vehículo está en mantenimiento y no puede ser asignado.");
    }

    // 1. Check conflicts in CalendarEvent
    List<CalendarEvent> eventConflicts =
        calendarEventRepository.findConflictingVehicles(vehicleId, start, end);
    boolean hasEventConflict = eventConflicts.stream().anyMatch(e -> !e.getId().equals(excludeId));

    if (hasEventConflict) {
      throw new RuntimeException(
          "Conflicto de horario: El vehículo ya está reservado o en tránsito en CalendarEvent.");
    }

    // 2. Check conflicts in Visit (Agnostic Aggregation)
    List<Visit> visitConflicts = visitRepository.findConflictingVehicles(vehicleId, start, end);
    boolean hasVisitConflict = visitConflicts.stream().anyMatch(v -> !v.getId().equals(excludeId));

    if (hasVisitConflict) {
      throw new RuntimeException(
          "Conflicto de horario: El vehículo ya está reservado o en tránsito en Visits (Reschedule).");
    }
  }

  // ─── Asignación de Vehículos ─────────────────────────────────────────────

  /** Asigna un vehículo a un CalendarEvent. */
  public CalendarEvent assignVehicleToVisit(
      String eventId, String vehicleId, Integer travelGo, Integer travelBack) {
    CalendarEvent event =
        calendarEventRepository
            .findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Evento de calendario no encontrado"));

    LocalDateTime occupancyStart =
        event.getStartTime().minusMinutes(travelGo != null ? travelGo : 0);
    LocalDateTime occupancyEnd =
        event.getEndTime().plusMinutes(travelBack != null ? travelBack : 0);

    checkVehicleAvailability(vehicleId, occupancyStart, occupancyEnd, eventId);

    event.setVehicleId(vehicleId);
    event.setTravelTimeGo(travelGo);
    event.setTravelTimeBack(travelBack);
    event.setStatus(CalendarEvent.EventStatus.CONFIRMED);

    return calendarEventRepository.save(event);
  }

  /**
   * Asigna un vehículo a una Visit (Soporte para Reschedule). Mantiene la cohesión sin duplicar la
   * lógica de negocio en el servicio del otro dev.
   */
  public Visit assignVehicleToRescheduledVisit(
      String visitId, String vehicleId, Integer travelGo, Integer travelBack) {
    Visit visit =
        visitRepository
            .findById(visitId)
            .orElseThrow(() -> new ResourceNotFoundException("Visita no encontrada"));

    LocalDateTime occupancyStart =
        visit.getDateTime().minusMinutes(travelGo != null ? travelGo : 0);
    LocalDateTime occupancyEnd =
        visit.getEndTime().plusMinutes(travelBack != null ? travelBack : 0);

    checkVehicleAvailability(vehicleId, occupancyStart, occupancyEnd, visitId);

    visit.setVehicleId(vehicleId);
    visit.setTravelTimeGo(travelGo);
    visit.setTravelTimeBack(travelBack);
    // Nota: El otro dev usa VisitStatus, asumimos que la asignación confirma la logística.

    return visitRepository.save(visit);
  }

  public CalendarEvent getVisitWithAssignment(String visitId) {
    return calendarEventRepository
        .findById(visitId)
        .orElseThrow(() -> new ResourceNotFoundException("Visita no encontrada"));
  }
}
