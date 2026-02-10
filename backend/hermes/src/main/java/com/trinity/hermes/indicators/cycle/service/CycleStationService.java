package com.trinity.hermes.indicators.cycle.service;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.cycle.dto.CycleStationDTO;
import com.trinity.hermes.indicators.cycle.entity.CycleStation;
import com.trinity.hermes.indicators.cycle.repository.CycleStationRepository;
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
public class CycleStationService {

  private final CycleStationRepository repository;

  /** Get all cycle stations */
  @Transactional(readOnly = true)
  public List<CycleStationDTO> getAllCycleStations(Integer limit) {
    log.info("Fetching all cycle stations with limit: {}", LogSanitizer.sanitizeLog(limit));

    List<CycleStation> stations;
    if (limit != null && limit > 0) {
      stations = repository.findAll().stream().limit(limit).collect(Collectors.toList());
    } else {
      stations = repository.findAll();
    }

    return stations.stream().map(this::convertToDTO).collect(Collectors.toList());
  }

  /** Get stations with available bikes */
  @Transactional(readOnly = true)
  public List<CycleStationDTO> getAvailableBikeStations() {
    log.info("Fetching stations with available bikes");
    return repository.findAvailableBikeStations().stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }

  /** Get stations with available docks */
  @Transactional(readOnly = true)
  public List<CycleStationDTO> getAvailableDockStations() {
    log.info("Fetching stations with available docks");
    return repository.findAvailableDockStations().stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }

  /** Get cycle station statistics */
  @Transactional(readOnly = true)
  public CycleStatistics getCycleStatistics() {
    List<CycleStation> stations = repository.findByIsInstalledTrue();

    CycleStatistics stats = new CycleStatistics();
    stats.setTotalStations(stations.size());

    List<Integer> bikeCounts =
        stations.stream()
            .map(CycleStation::getNumBikesAvailable)
            .filter(count -> count != null)
            .collect(Collectors.toList());

    if (!bikeCounts.isEmpty()) {
      DoubleSummaryStatistics bikeStats =
          bikeCounts.stream().mapToDouble(Integer::doubleValue).summaryStatistics();

      stats.setAverageBikesAvailable(bikeStats.getAverage());
      stats.setTotalBikesAvailable((int) bikeStats.getSum());
      stats.setMaxBikesAtStation((int) bikeStats.getMax());
    }

    List<Integer> dockCounts =
        stations.stream()
            .map(CycleStation::getNumDocksAvailable)
            .filter(count -> count != null)
            .collect(Collectors.toList());

    if (!dockCounts.isEmpty()) {
      DoubleSummaryStatistics dockStats =
          dockCounts.stream().mapToDouble(Integer::doubleValue).summaryStatistics();

      stats.setAverageDocksAvailable(dockStats.getAverage());
      stats.setTotalDocksAvailable((int) dockStats.getSum());
    }

    // Calculate average occupancy rate
    Double avgOccupancy = repository.findAverageOccupancyRate();
    stats.setAverageOccupancyRate(avgOccupancy != null ? avgOccupancy : 0.0);

    // Count stations by status
    long rentingStations = stations.stream().filter(CycleStation::getIsRenting).count();
    long returningStations = stations.stream().filter(CycleStation::getIsReturning).count();

    stats.setStationsRenting((int) rentingStations);
    stats.setStationsReturning((int) returningStations);

    return stats;
  }

  /** Convert entity to DTO */
  private CycleStationDTO convertToDTO(CycleStation entity) {
    CycleStationDTO dto = new CycleStationDTO();
    dto.setStationId(entity.getStationId());
    dto.setName(entity.getName());
    dto.setAddress(entity.getAddress());
    dto.setCapacity(entity.getCapacity());
    dto.setNumBikesAvailable(entity.getNumBikesAvailable());
    dto.setNumDocksAvailable(entity.getNumDocksAvailable());
    dto.setIsInstalled(entity.getIsInstalled());
    dto.setIsRenting(entity.getIsRenting());
    dto.setIsReturning(entity.getIsReturning());
    dto.setLastReported(entity.getLastReported());
    dto.setLastReportedDt(entity.getLastReportedDt());
    dto.setLat(entity.getLat());
    dto.setLon(entity.getLon());
    dto.setOccupancyRate(dto.calculateOccupancyRate());
    return dto;
  }

  /** Cycle Statistics DTO */
  @lombok.Data
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class CycleStatistics {
    private int totalStations;
    private Double averageBikesAvailable;
    private Integer totalBikesAvailable;
    private Integer maxBikesAtStation;
    private Double averageDocksAvailable;
    private Integer totalDocksAvailable;
    private Double averageOccupancyRate;
    private Integer stationsRenting;
    private Integer stationsReturning;
  }
}
