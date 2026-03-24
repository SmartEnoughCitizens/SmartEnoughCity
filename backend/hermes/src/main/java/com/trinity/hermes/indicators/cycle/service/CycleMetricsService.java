package com.trinity.hermes.indicators.cycle.service;

import com.trinity.hermes.indicators.cycle.dto.HourlyNetworkProfileDTO;
import com.trinity.hermes.indicators.cycle.dto.NetworkSummaryDTO;
import com.trinity.hermes.indicators.cycle.dto.RebalanceSuggestionDTO;
import com.trinity.hermes.indicators.cycle.dto.RegionMetricsDTO;
import com.trinity.hermes.indicators.cycle.dto.StationClassificationDTO;
import com.trinity.hermes.indicators.cycle.dto.StationHourlyUsageDTO;
import com.trinity.hermes.indicators.cycle.dto.StationLiveDTO;
import com.trinity.hermes.indicators.cycle.dto.StationODPairDTO;
import com.trinity.hermes.indicators.cycle.dto.StationRankingDTO;
import com.trinity.hermes.indicators.cycle.entity.DublinBikesStation;
import com.trinity.hermes.indicators.cycle.repository.DublinBikesSnapshotRepository;
import com.trinity.hermes.indicators.cycle.repository.DublinBikesStationRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CycleMetricsService {

  private final DublinBikesStationRepository stationRepository;
  private final DublinBikesSnapshotRepository snapshotRepository;

  // -------------------------------------------------------------------------
  // Live Station Data
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<StationLiveDTO> getLiveStations() {
    log.debug("Fetching live station data for all stations");
    List<Object[]> rows = snapshotRepository.findLatestSnapshotPerStation();
    return rows.stream().map(this::mapToStationLiveDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public NetworkSummaryDTO getNetworkSummary() {
    log.debug("Fetching network summary");
    Object[] row = firstRow(snapshotRepository.findNetworkSummary());
    if (row == null) {
      return new NetworkSummaryDTO();
    }

    int totalStations = toIntOrDefault(row[0], 0);
    int totalBikes = toIntOrDefault(row[1], 0);
    int totalDocks = toIntOrDefault(row[2], 0);
    int disabledBikes = toIntOrDefault(row[3], 0);
    int disabledDocks = toIntOrDefault(row[4], 0);
    int emptyStations = toIntOrDefault(row[5], 0);
    int fullStations = toIntOrDefault(row[6], 0);
    double avgFullness = toDoubleOrDefault(row[7], 0.0);
    Instant latestTimestamp = toInstant(row[8]);

    NetworkSummaryDTO dto = new NetworkSummaryDTO();
    dto.setTotalStations(totalStations);
    dto.setActiveStations(totalStations);
    dto.setTotalBikesAvailable(totalBikes);
    dto.setTotalDocksAvailable(totalDocks);
    dto.setTotalDisabledBikes(disabledBikes);
    dto.setTotalDisabledDocks(disabledDocks);
    dto.setEmptyStations(emptyStations);
    dto.setFullStations(fullStations);
    dto.setAvgNetworkFullnessPct(avgFullness);
    dto.setRebalancingNeedCount(emptyStations);
    dto.setDataAsOf(latestTimestamp);
    return dto;
  }

  @Transactional(readOnly = true)
  public List<RegionMetricsDTO> getRegionMetrics() {
    log.debug("Fetching region-level metrics");
    List<Object[]> rows = snapshotRepository.findRegionMetrics();
    return rows.stream()
        .map(
            row -> {
              RegionMetricsDTO dto = new RegionMetricsDTO();
              dto.setRegionId((String) row[0]);
              dto.setStationCount(toLong(row[1]));
              dto.setTotalCapacity(toLong(row[2]));
              dto.setAvgUsageRate(toDouble(row[3]));
              dto.setAvgAvailableBikes(toDouble(row[4]));
              dto.setAvgAvailableDocks(toDouble(row[5]));
              dto.setEmptyStations(toLong(row[6]));
              dto.setFullStations(toLong(row[7]));
              return dto;
            })
        .collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Station Rankings
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<StationRankingDTO> getBusiestStations(int limit) {
    List<Object[]> rows = snapshotRepository.findBusiestStations(limit);
    return rows.stream().map(this::mapToRankingDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<StationRankingDTO> getLeastUsedStations(int limit) {
    List<Object[]> rows = snapshotRepository.findLeastUsedStations(limit);
    return rows.stream().map(this::mapToRankingDTO).collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Rebalancing Suggestions
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<RebalanceSuggestionDTO> getRebalancingSuggestions(int limit) {
    log.debug("Fetching rebalancing suggestions, limit {}", limit);
    List<Object[]> rows = snapshotRepository.findRebalancingSuggestions(limit);
    return rows.stream().map(this::mapToRebalanceSuggestionDTO).collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Demand Analysis

  @Transactional(readOnly = true)
  public List<HourlyNetworkProfileDTO> getNetworkHourlyProfile(int days) {
    log.debug("Fetching network hourly profile for last {} days", days);
    List<Object[]> rows = snapshotRepository.findNetworkHourlyProfile(days);
    return rows.stream().map(this::mapToHourlyNetworkProfileDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<StationClassificationDTO> getStationClassification(int days) {
    log.debug("Fetching station classification for last {} days", days);
    List<Object[]> rows = snapshotRepository.findStationClassification(days);
    return rows.stream().map(this::mapToStationClassificationDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<StationODPairDTO> getODPairs(int days, int limit) {
    log.debug("Fetching OD pairs for last {} days, limit {}", days, limit);
    List<Object[]> rows = snapshotRepository.findODPairs(days, limit);
    return rows.stream().map(this::mapToODPairDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<StationHourlyUsageDTO> getStationHourlyUsage(int days, int stationLimit) {
    log.debug(
        "Fetching station hourly usage for last {} days, top {} stations", days, stationLimit);
    List<Object[]> rows = snapshotRepository.findStationHourlyUsage(days, stationLimit);
    return rows.stream().map(this::mapToStationHourlyUsageDTO).collect(Collectors.toList());
  }

  // Fleet
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<DublinBikesStation> getAllStations() {
    return stationRepository.findAll();
  }

  // -------------------------------------------------------------------------
  // Mapping helpers
  // -------------------------------------------------------------------------

  private StationLiveDTO mapToStationLiveDTO(Object[] row) {
    Integer stationId = toInt(row[0]);
    Integer capacity = toInt(row[6]);
    Integer availableBikes = toInt(row[8]);
    Integer availableDocks = toInt(row[9]);

    double bikeAvailabilityPct = 0.0;
    double dockAvailabilityPct = 0.0;
    if (capacity != null && capacity > 0) {
      if (availableBikes != null) bikeAvailabilityPct = (double) availableBikes / capacity * 100.0;
      if (availableDocks != null) dockAvailabilityPct = (double) availableDocks / capacity * 100.0;
    }

    String statusColor =
        bikeAvailabilityPct < 20 ? "RED" : bikeAvailabilityPct < 40 ? "YELLOW" : "GREEN";

    StationLiveDTO dto = new StationLiveDTO();
    dto.setStationId(stationId);
    dto.setName((String) row[1]);
    dto.setShortName((String) row[2]);
    dto.setAddress((String) row[3]);
    dto.setLatitude(row[4] != null ? new BigDecimal(row[4].toString()) : null);
    dto.setLongitude(row[5] != null ? new BigDecimal(row[5].toString()) : null);
    dto.setCapacity(capacity);
    dto.setRegionId((String) row[7]);
    dto.setAvailableBikes(availableBikes);
    dto.setAvailableDocks(availableDocks);
    dto.setDisabledBikes(row[10] != null ? toInt(row[10]) : null);
    dto.setDisabledDocks(row[11] != null ? toInt(row[11]) : null);
    dto.setIsInstalled((Boolean) row[12]);
    dto.setIsRenting((Boolean) row[13]);
    dto.setIsReturning((Boolean) row[14]);
    dto.setLastReported(toInstant(row[15]));
    dto.setSnapshotTimestamp(toInstant(row[16]));
    dto.setBikeAvailabilityPct(bikeAvailabilityPct);
    dto.setDockAvailabilityPct(dockAvailabilityPct);
    dto.setStatusColor(statusColor);
    dto.setIsEmpty(availableBikes != null && availableBikes == 0);
    dto.setIsFull(availableDocks != null && availableDocks == 0);
    return dto;
  }

  private StationRankingDTO mapToRankingDTO(Object[] row) {
    StationRankingDTO dto = new StationRankingDTO();
    dto.setStationId(toInt(row[0]));
    dto.setName((String) row[1]);
    dto.setAvgUsageRate(toDouble(row[2]));
    return dto;
  }

  private RebalanceSuggestionDTO mapToRebalanceSuggestionDTO(Object[] row) {
    RebalanceSuggestionDTO dto = new RebalanceSuggestionDTO();
    dto.setSourceStationId(toInt(row[0]));
    dto.setSourceName((String) row[1]);
    dto.setSourceLat(row[2] != null ? new BigDecimal(row[2].toString()) : null);
    dto.setSourceLon(row[3] != null ? new BigDecimal(row[3].toString()) : null);
    dto.setSourceBikes(toInt(row[4]));
    dto.setTargetStationId(toInt(row[5]));
    dto.setTargetName((String) row[6]);
    dto.setTargetLat(row[7] != null ? new BigDecimal(row[7].toString()) : null);
    dto.setTargetLon(row[8] != null ? new BigDecimal(row[8].toString()) : null);
    dto.setTargetCapacity(toInt(row[9]));
    dto.setDistanceKm(toDouble(row[10]));
    return dto;
  }

  private HourlyNetworkProfileDTO mapToHourlyNetworkProfileDTO(Object[] row) {
    HourlyNetworkProfileDTO dto = new HourlyNetworkProfileDTO();
    dto.setHourOfDay(toIntOrDefault(row[0], 0));
    dto.setAvgUsageRate(toDoubleOrDefault(row[1], 0.0));
    dto.setStationCount(toLong(row[2]));
    return dto;
  }

  private StationClassificationDTO mapToStationClassificationDTO(Object[] row) {
    StationClassificationDTO dto = new StationClassificationDTO();
    dto.setStationId(toIntOrDefault(row[0], 0));
    dto.setName((String) row[1]);
    dto.setPeakHour(toIntOrDefault(row[2], 0));
    dto.setPeakUsage(toDoubleOrDefault(row[3], 0.0));
    dto.setClassification((String) row[4]);
    return dto;
  }

  private StationHourlyUsageDTO mapToStationHourlyUsageDTO(Object[] row) {
    StationHourlyUsageDTO dto = new StationHourlyUsageDTO();
    dto.setStationId(toIntOrDefault(row[0], 0));
    dto.setName((String) row[1]);
    dto.setHourOfDay(toIntOrDefault(row[2], 0));
    dto.setAvgUsageRate(toDoubleOrDefault(row[3], 0.0));
    return dto;
  }

  private StationODPairDTO mapToODPairDTO(Object[] row) {
    StationODPairDTO dto = new StationODPairDTO();
    dto.setOriginStationId(toIntOrDefault(row[0], 0));
    dto.setOriginName((String) row[1]);
    dto.setOriginLat(row[2] != null ? new BigDecimal(row[2].toString()) : null);
    dto.setOriginLon(row[3] != null ? new BigDecimal(row[3].toString()) : null);
    dto.setDestStationId(toIntOrDefault(row[4], 0));
    dto.setDestName((String) row[5]);
    dto.setDestLat(row[6] != null ? new BigDecimal(row[6].toString()) : null);
    dto.setDestLon(row[7] != null ? new BigDecimal(row[7].toString()) : null);
    dto.setEstimatedTrips(toIntOrDefault(row[8], 0));
    dto.setDistanceKm(toDoubleOrDefault(row[9], 0.0));
    return dto;
  }

  // -------------------------------------------------------------------------
  // Type conversion utilities
  // -------------------------------------------------------------------------

  private Integer toInt(Object value) {
    if (value == null) return null;
    if (value instanceof Integer i) return i;
    if (value instanceof Long l) return l.intValue();
    if (value instanceof Number n) return n.intValue();
    return Integer.parseInt(value.toString());
  }

  private Long toLong(Object value) {
    if (value == null) return null;
    if (value instanceof Long l) return l;
    if (value instanceof Number n) return n.longValue();
    return Long.parseLong(value.toString());
  }

  private Double toDouble(Object value) {
    if (value == null) return null;
    if (value instanceof Double d) return d;
    if (value instanceof Number n) return n.doubleValue();
    return Double.parseDouble(value.toString());
  }

  private Instant toInstant(Object value) {
    if (value == null) return null;
    if (value instanceof Instant i) return i;
    if (value instanceof Timestamp ts) return ts.toInstant();
    if (value instanceof java.util.Date d) return d.toInstant();
    return Instant.parse(value.toString());
  }

  private int toIntOrDefault(Object value, int defaultValue) {
    Integer result = toInt(value);
    return result != null ? result : defaultValue;
  }

  private double toDoubleOrDefault(Object value, double defaultValue) {
    Double result = toDouble(value);
    return result != null ? result : defaultValue;
  }

  private Object[] firstRow(List<Object[]> rows) {
    return (rows == null || rows.isEmpty()) ? null : rows.get(0);
  }
}
