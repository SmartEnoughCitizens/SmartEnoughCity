package com.trinity.hermes.indicators.bus.service;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.bus.dto.BusTripUpdateDTO;
import com.trinity.hermes.indicators.bus.entity.BusTripUpdate;
import com.trinity.hermes.indicators.bus.repository.BusTripUpdateRepository;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusTripUpdateService {

  private final BusTripUpdateRepository repository;

  /** Get all bus trip updates for a specific route */
  @Transactional(readOnly = true)
  public List<BusTripUpdateDTO> getBusTripUpdatesByRoute(String routeId, Integer limit) {
    log.info("Fetching bus trip updates for route: {}", LogSanitizer.sanitizeLog(routeId));

    List<BusTripUpdate> updates = repository.findByRouteIdOrderByIdDesc(routeId);

    if (limit != null && limit > 0) {
      updates = updates.stream().limit(limit).collect(Collectors.toList());
    }

    return updates.stream().map(this::convertToDTO).collect(Collectors.toList());
  }

  /** Get all distinct route IDs */
  @Transactional(readOnly = true)
  public List<String> getAllRoutes() {
    return repository.findAllDistinctRouteIds();
  }

  /** Get delay statistics for a route */
  @Transactional(readOnly = true)
  public DelayStatistics getDelayStatistics(String routeId) {
    List<BusTripUpdate> updates = repository.findByRouteId(routeId);

    List<Integer> arrivalDelays =
        updates.stream()
            .map(BusTripUpdate::getArrivalDelay)
            .filter(delay -> delay != null)
            .collect(Collectors.toList());

    List<Integer> departureDelays =
        updates.stream()
            .map(BusTripUpdate::getDepartureDelay)
            .filter(delay -> delay != null)
            .collect(Collectors.toList());

    DelayStatistics stats = new DelayStatistics();
    stats.setRouteId(routeId);
    stats.setTotalTrips(updates.size());

    if (!arrivalDelays.isEmpty()) {
      DoubleSummaryStatistics arrivalStats =
          arrivalDelays.stream().mapToDouble(Integer::doubleValue).summaryStatistics();
      stats.setAverageArrivalDelay(arrivalStats.getAverage());
      stats.setMaxArrivalDelay(arrivalStats.getMax());
      stats.setMinArrivalDelay(arrivalStats.getMin());
    }

    if (!departureDelays.isEmpty()) {
      DoubleSummaryStatistics departureStats =
          departureDelays.stream().mapToDouble(Integer::doubleValue).summaryStatistics();
      stats.setAverageDepartureDelay(departureStats.getAverage());
      stats.setMaxDepartureDelay(departureStats.getMax());
      stats.setMinDepartureDelay(departureStats.getMin());
    }

    return stats;
  }

  /** Get all bus trip updates (for recommendation engine) */
  @Transactional(readOnly = true)
  public List<BusTripUpdateDTO> getAllBusTripUpdates(Integer limit) {
    log.info("Fetching all bus trip updates with limit: {}", LogSanitizer.sanitizeLog(limit));

    List<BusTripUpdate> updates;
    if (limit != null && limit > 0) {
      updates = repository.findAll().stream().limit(limit).collect(Collectors.toList());
    } else {
      updates = repository.findAll();
    }

    return updates.stream().map(this::convertToDTO).collect(Collectors.toList());
  }

  /** Convert entity to DTO */
  private BusTripUpdateDTO convertToDTO(BusTripUpdate entity) {
    return new BusTripUpdateDTO(
        entity.getId(),
        entity.getEntityId(),
        entity.getTripId(),
        entity.getRouteId(),
        entity.getStartTime(),
        entity.getStartDate(),
        entity.getStopSequence(),
        entity.getStopId(),
        entity.getArrivalDelay(),
        entity.getDepartureDelay(),
        entity.getScheduleRelationship());
  }

  /** Delay Statistics DTO */
  @lombok.Data
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class DelayStatistics {
    private String routeId;
    private int totalTrips;
    private Double averageArrivalDelay;
    private Double maxArrivalDelay;
    private Double minArrivalDelay;
    private Double averageDepartureDelay;
    private Double maxDepartureDelay;
    private Double minDepartureDelay;
  }
}
