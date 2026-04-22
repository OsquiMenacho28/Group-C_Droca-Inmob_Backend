package com.inmobiliaria.visit_calendar_service.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Entidad principal que representa una Visita/Cita inmobiliaria. */
@Document(collection = "visits")
public class Visit {

  @Id private String id;

  /** ID del inmueble al que corresponde la visita */
  private String propertyId;

  /** ID del cliente que solicitó la visita */
  private String clientId;

  /**
   * ID del agente actualmente asignado a la visita. Este campo se actualiza cuando se acepta una
   * reasignación.
   */
  private String agentId;

  /** ID del vehículo asignado a la visita */
  private String vehicleId;

  /** Fecha y hora de inicio programada para la visita */
  private LocalDateTime dateTime;

  /** Fecha y hora de fin programada para la visita */
  private LocalDateTime endTime;

  /** Tiempo de desplazamiento de ida (en minutos) */
  private Integer travelTimeGo;

  /** Tiempo de desplazamiento de vuelta (en minutos) */
  private Integer travelTimeBack;

  /** Estado de la visita: PROGRAMADA, CANCELADA, COMPLETADA */
  private VisitStatus status;

  /** Notas adicionales sobre la visita */
  private String notes;

  /** Fecha en que se creó la solicitud de visita */
  private LocalDateTime createdAt;

  /** Historial de todas las reasignaciones */
  private List<ReassignmentHistory> reassignmentHistory = new ArrayList<>();

  /** ID de la visita original si fue reprogramada */
  private String originVisitId;

  /** Historial de reprogramaciones */
  private List<ReschedulingHistory> reschedulingHistory = new ArrayList<>();

  public enum VisitStatus {
    SCHEDULED,
    CANCELLED,
    COMPLETED
  }

  // ── Constructors ──────────────────────────────────────────────────────────

  public Visit() {
    this.createdAt = LocalDateTime.now();
    this.reassignmentHistory = new ArrayList<>();
    this.travelTimeGo = 0;
    this.travelTimeBack = 0;
  }

  // ── Getters & Setters ─────────────────────────────────────────────────────

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPropertyId() {
    return propertyId;
  }

  public void setPropertyId(String propertyId) {
    this.propertyId = propertyId;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public LocalDateTime getDateTime() {
    return dateTime;
  }

  public void setDateTime(LocalDateTime dateTime) {
    this.dateTime = dateTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public Integer getTravelTimeGo() {
    return travelTimeGo;
  }

  public void setTravelTimeGo(Integer travelTimeGo) {
    this.travelTimeGo = travelTimeGo;
  }

  public Integer getTravelTimeBack() {
    return travelTimeBack;
  }

  public void setTravelTimeBack(Integer travelTimeBack) {
    this.travelTimeBack = travelTimeBack;
  }

  public VisitStatus getStatus() {
    return status;
  }

  public void setStatus(VisitStatus status) {
    this.status = status;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public List<ReassignmentHistory> getReassignmentHistory() {
    return reassignmentHistory;
  }

  public void setReassignmentHistory(List<ReassignmentHistory> reassignmentHistory) {
    this.reassignmentHistory = reassignmentHistory;
  }

  public String getOriginVisitId() {
    return originVisitId;
  }

  public void setOriginVisitId(String originVisitId) {
    this.originVisitId = originVisitId;
  }

  public List<ReschedulingHistory> getReschedulingHistory() {
    return reschedulingHistory;
  }

  public void setReschedulingHistory(List<ReschedulingHistory> reschedulingHistory) {
    this.reschedulingHistory = reschedulingHistory;
  }
}
