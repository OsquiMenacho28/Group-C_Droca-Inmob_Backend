package com.inmobiliaria.operation_service.service;

import com.inmobiliaria.operation_service.client.PropertyClient;
import com.inmobiliaria.operation_service.client.VisitClient;
import com.inmobiliaria.operation_service.repository.OperationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

  private final PropertyClient propertyClient;
  private final VisitClient visitClient;
  private final OperationRepository operationRepository;

  // Variables para Caché en memoria (TTL: 2 minutos)
  private DashboardSummaryResponse cachedSummary;
  private Instant summaryCacheTime = Instant.MIN;
  private Map<String, Long> cachedDistribution;
  private Instant distributionCacheTime = Instant.MIN;

  public DashboardSummaryResponse getResumen() {
    if (Instant.now().isBefore(summaryCacheTime.plus(2, ChronoUnit.MINUTES))
        && cachedSummary != null) {
      return cachedSummary;
    }

    long activeProperties = 0;
    try {
      var reportResponse = propertyClient.getInventoryReport();
      if (reportResponse != null && reportResponse.getData() != null) {
        var totals = reportResponse.getData().totalsByStatus();
        if (totals != null) {
          activeProperties =
              totals.entrySet().stream()
                  .filter(
                      e ->
                          List.of("DISPONIBLE", "EN_NEGOCIACION", "RESERVADO").contains(e.getKey()))
                  .mapToLong(Map.Entry::getValue)
                  .sum();
        }
      }
    } catch (Exception e) {
      log.warn("No se pudo obtener el reporte de inventario: {}", e.getMessage());
    }

    long pendingVisits = 0;
    try {
      Instant now = Instant.now();
      Instant endOfWeek = now.plus(7, ChronoUnit.DAYS);
      var calendarResponse = visitClient.getCalendar(now, endOfWeek);
      if (calendarResponse != null && calendarResponse.getData() != null) {
        pendingVisits = calendarResponse.getData().totalEvents();
      }
    } catch (Exception e) {
      log.warn("No se pudo obtener el calendario de visitas: {}", e.getMessage());
    }

    long activeOperations = operationRepository.countByStatusIn(List.of("PENDING", "ACTIVE"));

    cachedSummary = new DashboardSummaryResponse(activeProperties, pendingVisits, activeOperations);
    summaryCacheTime = Instant.now();
    return cachedSummary;
  }

  public Map<String, Long> getDistribucionEstados() {
    if (Instant.now().isBefore(distributionCacheTime.plus(2, ChronoUnit.MINUTES))
        && cachedDistribution != null) {
      return cachedDistribution;
    }

    Map<String, Long> distribution = Map.of();
    try {
      var reportResponse = propertyClient.getInventoryReport();
      if (reportResponse != null
          && reportResponse.getData() != null
          && reportResponse.getData().totalsByStatus() != null) {
        distribution = reportResponse.getData().totalsByStatus();
      }
    } catch (Exception e) {
      log.warn("No se pudo obtener la distribución de estados: {}", e.getMessage());
    }

    cachedDistribution = distribution;
    distributionCacheTime = Instant.now();
    return cachedDistribution;
  }

  public record DashboardSummaryResponse(
      long activeProperties, long weeklyVisits, long activeOperations) {}
}
