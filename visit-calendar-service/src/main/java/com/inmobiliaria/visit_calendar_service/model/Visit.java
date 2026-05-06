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

  /** Nombre descriptivo del inmueble */
  private String propertyName;

  /** Dirección del inmueble */
  private String propertyAddress;

  /** ID del cliente que solicitó la visita */
  private String clientId;

  /** Nombre del cliente que solicitó la visita */
  private String clientName;

  /**
   * ID del agente actualmente asignado a la visita. Este campo se actualiza cuando se acepta una
   * reasignación.
   */
  private String agentId;

  /** Nombre completo del agente */
  private String agentName;

  /** ID del vehículo asignado a la visita */
  private String vehicleId;

  /** Fecha y hora de inicio programada para la visita */
  private LocalDateTime startTime;

  /** Fecha y hora de fin programada para la visita */
  private LocalDateTime endTime;

  /** Tiempo de desplazamiento de ida (en minutos) */
  private Integer travelTimeGo;

  /** Tiempo de desplazamiento de vuelta (en minutos) */
  private Integer travelTimeBack;

  /** Tipo de evento: VISIT (visita de agente), CLIENT_REQUEST (solicitud de cliente) */
  private EventType type;

  /** Estado de la visita: SCHEDULED, CONFIRMED, CANCELLED, COMPLETED */
  private EventStatus status;

  /** Notas adicionales sobre la visita */
  private String notes;

  /** Indica si esta visita es propiedad del agente autenticado (para UI) */
  private Boolean ownEvent;

  /** Fecha en que se creó la solicitud de visita */
  private LocalDateTime createdAt;

  /** Historial de todas las reasignaciones */
  private List<ReassignmentHistory> reassignmentHistory;

  /** ID de la visita original si fue reprogramada */
  private String originVisitId;

  /** Historial de reprogramaciones */
  private List<ReschedulingHistory> reschedulingHistory;

  /**
   * Tipo de evento. Valores posibles: VISIT (visita de agente), CLIENT_REQUEST (solicitud de
   * cliente)
   */
  public enum EventType {
    VISIT, // Visita programada por un agente
    CLIENT_REQUEST // Solicitud de visita iniciada por un cliente
  }

  /** Estado del evento. Valores posibles: SCHEDULED, CONFIRMED, CANCELLED, COMPLETED */
  public enum EventStatus {
    SCHEDULED, // Programada, pendiente de confirmación
    CONFIRMED, // Confirmada
    CANCELLED, // Cancelada
    COMPLETED, // Completada
    REALIZADA // Completada (sin resultado registrado)
  }

  // Resultado de la visita. Valores posibles: INTERESADO, NO_INTERESADO, PENDIENTE

  public enum ResultadoVisita {
    INTERESADO,
    NO_INTERESADO,
    PENDIENTE
  }

  private ResultadoVisita resultado;
  private String observaciones;
  private LocalDateTime fechaRegistroResultado;

  // ── Constructors ──────────────────────────────────────────────────────────

  public Visit() {
    this.createdAt = LocalDateTime.now();
    this.reassignmentHistory = new ArrayList<>();
    this.travelTimeGo = 0;
    this.travelTimeBack = 0;
    this.ownEvent = false;
    this.reschedulingHistory = new ArrayList<>();
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

  public String getPropertyName() {
    return propertyName;
  }

  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  public String getPropertyAddress() {
    return propertyAddress;
  }

  public void setPropertyAddress(String propertyAddress) {
    this.propertyAddress = propertyAddress;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public String getAgentName() {
    return agentName;
  }

  public void setAgentName(String agentName) {
    this.agentName = agentName;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
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

  public EventType getType() {
    return type;
  }

  public void setType(EventType type) {
    this.type = type;
  }

  public EventStatus getStatus() {
    return status;
  }

  public void setStatus(EventStatus status) {
    this.status = status;
  }

  public Boolean getOwnEvent() {
    return ownEvent;
  }

  public void setOwnEvent(Boolean ownEvent) {
    this.ownEvent = ownEvent;
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

  // Resultado de la visita
  public ResultadoVisita getResultado() {
    return resultado;
  }

  public void setResultado(ResultadoVisita resultado) {
    this.resultado = resultado;
  }

  public String getObservaciones() {
    return observaciones;
  }

  public void setObservaciones(String observaciones) {
    this.observaciones = observaciones;
  }

  public LocalDateTime getFechaRegistroResultado() {
    return fechaRegistroResultado;
  }

  public void setFechaRegistroResultado(LocalDateTime fechaRegistroResultado) {
    this.fechaRegistroResultado = fechaRegistroResultado;
  }
}
