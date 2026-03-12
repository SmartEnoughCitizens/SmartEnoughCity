package com.trinity.hermes.indicators.cycle.service;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.cycle.dto.NetworkKpiDTO;
import com.trinity.hermes.indicators.cycle.dto.NetworkSummaryDTO;
import com.trinity.hermes.indicators.cycle.dto.RegionMetricsDTO;
import com.trinity.hermes.indicators.cycle.dto.StationLiveDTO;
import com.trinity.hermes.indicators.cycle.dto.StationODPairDTO;
import com.trinity.hermes.indicators.cycle.dto.StationRankingDTO;
import com.trinity.hermes.indicators.cycle.dto.StationTimeSeriesDTO;
import com.trinity.hermes.indicators.cycle.entity.DublinBikesStation;
import com.trinity.hermes.indicators.cycle.repository.DublinBikesHistoryRepository;
import com.trinity.hermes.indicators.cycle.repository.DublinBikesSnapshotRepository;
import com.trinity.hermes.indicators.cycle.repository.DublinBikesStationRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
  private final DublinBikesHistoryRepository historyRepository;

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
    Object[] row = snapshotRepository.findNetworkSummary();
    if (row == null) {
      return new NetworkSummaryDTO();
    }

    int totalStations = toInt(row[0]);
    int totalBikes = toInt(row[1]);
    int totalDocks = toInt(row[2]);
    int disabledBikes = toInt(row[3]);
    int disabledDocks = toInt(row[4]);
    int emptyStations = toInt(row[5]);
    int fullStations = toInt(row[6]);
    double avgFullness = toDouble(row[7]);
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
    dto.setRebalancingNeedCount(emptyStations + fullStations);
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
  // Historical Time-series
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<StationTimeSeriesDTO> getStationTimeSeries(
      Integer stationId, String granularity, Instant from, Instant to) {
    log.debug(
        "Fetching {} time series for station {}",
        LogSanitizer.sanitizeLog(granularity),
        LogSanitizer.sanitizeLog(stationId));

    List<Object[]> rows =
        switch (granularity.toLowerCase(Locale.ROOT)) {
          case "hour" -> historyRepository.findHourlyTimeSeriesForStation(stationId, from, to);
          case "week" -> historyRepository.findWeeklyTimeSeriesForStation(stationId, from, to);
          default -> historyRepository.findDailyTimeSeriesForStation(stationId, from, to);
        };

    return rows.stream().map(this::mapToTimeSeriesDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<StationTimeSeriesDTO> getNetworkDailyTrend(int days) {
    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    List<Object[]> rows = historyRepository.findNetworkDailyTrend(since);
    return rows.stream().map(this::mapToTimeSeriesDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<StationTimeSeriesDTO> getNetworkMonthlyTrend(int months) {
    Instant since = Instant.now().minus(months * 30L, ChronoUnit.DAYS);
    List<Object[]> rows = historyRepository.findNetworkMonthlyTrend(since);
    return rows.stream().map(this::mapToTimeSeriesDTO).collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Usage Profiles
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public Map<Integer, Double> getHourlyUsageProfile(int days) {
    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    List<Object[]> rows = historyRepository.findHourlyUsageProfile(since);
    Map<Integer, Double> profile = new LinkedHashMap<>();
    for (Object[] row : rows) {
      profile.put(toInt(row[0]), toDouble(row[1]));
    }
    return profile;
  }

  @Transactional(readOnly = true)
  public Map<Integer, Double> getWeeklyUsageProfile(int days) {
    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    List<Object[]> rows = historyRepository.findWeeklyUsageProfile(since);
    Map<Integer, Double> profile = new LinkedHashMap<>();
    for (Object[] row : rows) {
      profile.put(toInt(row[0]), toDouble(row[1]));
    }
    return profile;
  }

  @Transactional(readOnly = true)
  public Map<String, Double> getWeekdayVsWeekendUsage(int days) {
    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    List<Object[]> rows = historyRepository.findWeekdayVsWeekendUsage(since);
    Map<String, Double> result = new LinkedHashMap<>();
    for (Object[] row : rows) {
      result.put((String) row[0], toDouble(row[1]));
    }
    return result;
  }

  // -------------------------------------------------------------------------
  // Station Rankings
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<StationRankingDTO> getBusiestStations(int days, int limit) {
    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    List<Object[]> rows = historyRepository.findBusiestStations(since, limit);
    return rows.stream().map(this::mapToRankingDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<StationRankingDTO> getLeastUsedStations(int days, int limit) {
    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    List<Object[]> rows = historyRepository.findLeastUsedStations(since, limit);
    return rows.stream().map(this::mapToRankingDTO).collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Network KPIs
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public NetworkKpiDTO getNetworkKpi() {
    log.debug("Computing network KPIs");

    Object[] imbalanceRow = snapshotRepository.findNetworkImbalanceScore();
    double imbalanceScore = imbalanceRow != null ? toDouble(imbalanceRow[0]) : 0.0;

    Object[] turnoverRow = historyRepository.findAvgHourlyTurnoverRate();
    double turnoverRate = turnoverRow != null ? toDouble(turnoverRow[0]) : 0.0;

    Instant dayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
    Object[] tripsRow = historyRepository.findTotalTripEstimate(dayStart, Instant.now());
    long dailyTrips = tripsRow != null ? toLong(tripsRow[0]) : 0L;

    Instant since90 = Instant.now().minus(90, ChronoUnit.DAYS);
    List<Object[]> dayTypeRows = historyRepository.findWeekdayVsWeekendUsage(since90);
    double weekdayRate = 0.0;
    double weekendRate = 0.0;
    for (Object[] row : dayTypeRows) {
      if ("weekday".equals(row[0])) weekdayRate = toDouble(row[1]);
      else if ("weekend".equals(row[0])) weekendRate = toDouble(row[1]);
    }

    Map<Integer, Double> hourlyProfile = getHourlyUsageProfile(30);
    List<StationTimeSeriesDTO> dailyTrend = getNetworkDailyTrend(30);

    Object[] summaryRow = snapshotRepository.findNetworkSummary();
    int rebalancingNeed = 0;
    if (summaryRow != null) {
      int emptyStations = toInt(summaryRow[5]);
      int fullStations = toInt(summaryRow[6]);
      rebalancingNeed = emptyStations + fullStations;
    }

    NetworkKpiDTO dto = new NetworkKpiDTO();
    dto.setRebalancingNeedCount(rebalancingNeed);
    dto.setNetworkImbalanceScore(imbalanceScore);
    dto.setAvgHourlyTurnoverRate(turnoverRate);
    dto.setDailyTripsEstimate(dailyTrips);
    dto.setWeekdayAvgUsageRate(weekdayRate);
    dto.setWeekendAvgUsageRate(weekendRate);
    dto.setHourlyUsageProfile(hourlyProfile);
    dto.setDailyTrend(dailyTrend);
    return dto;
  }

  // -------------------------------------------------------------------------
  // Origin-Destination Heatmap
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<StationODPairDTO> getODHeatmap(int limit) {
    YearMonth lastMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(1);
    Instant from = lastMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = lastMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant();
    log.debug("Computing OD heatmap for {} ({} to {}), top {} pairs", lastMonth, from, to, limit);
    List<Object[]> rows = historyRepository.findODPairs(from, to, limit);
    return rows.stream().map(this::mapToODPairDTO).collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
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
    // Column order from findLatestSnapshotPerStation:
    // 0:station_id, 1:name, 2:short_name, 3:address, 4:latitude, 5:longitude,
    // 6:capacity, 7:region_id, 8:available_bikes, 9:available_docks,
    // 10:disabled_bikes, 11:disabled_docks, 12:is_installed, 13:is_renting,
    // 14:is_returning, 15:last_reported, 16:snapshot_timestamp

    Integer stationId = toInt(row[0]);
    Integer capacity = toInt(row[6]);
    Integer availableBikes = toInt(row[8]);
    Integer availableDocks = toInt(row[9]);

    double bikeAvailabilityPct = 0.0;
    double dockAvailabilityPct = 0.0;
    if (capacity != null && capacity > 0) {
      if (availableBikes != null) {
        bikeAvailabilityPct = (double) availableBikes / capacity * 100.0;
      }
      if (availableDocks != null) {
        dockAvailabilityPct = (double) availableDocks / capacity * 100.0;
      }
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

  private StationTimeSeriesDTO mapToTimeSeriesDTO(Object[] row) {
    // 0:period, 1:avg_available_bikes, 2:avg_available_docks, 3:usage_rate_pct
    StationTimeSeriesDTO dto = new StationTimeSeriesDTO();
    dto.setPeriod(toInstant(row[0]));
    dto.setAvgAvailableBikes(toDouble(row[1]));
    dto.setAvgAvailableDocks(toDouble(row[2]));
    dto.setUsageRatePct(toDouble(row[3]));
    return dto;
  }

  private StationRankingDTO mapToRankingDTO(Object[] row) {
    // 0:station_id, 1:name, 2:region_id, 3:capacity, 4:avg_usage_rate,
    // 5:avg_available_bikes, 6:avg_available_docks, 7:empty_event_count, 8:full_event_count
    StationRankingDTO dto = new StationRankingDTO();
    dto.setStationId(toInt(row[0]));
    dto.setName((String) row[1]);
    dto.setRegionId((String) row[2]);
    dto.setCapacity(toInt(row[3]));
    dto.setAvgUsageRate(toDouble(row[4]));
    dto.setAvgAvailableBikes(toDouble(row[5]));
    dto.setAvgAvailableDocks(toDouble(row[6]));
    dto.setEmptyEventCount(toLong(row[7]));
    dto.setFullEventCount(toLong(row[8]));
    return dto;
  }

  private StationODPairDTO mapToODPairDTO(Object[] row) {
    // 0:origin_station_id, 1:origin_name, 2:origin_lat, 3:origin_lon,
    // 4:dest_station_id, 5:dest_name, 6:dest_lat, 7:dest_lon, 8:estimated_trips
    StationODPairDTO dto = new StationODPairDTO();
    dto.setOriginStationId(toInt(row[0]));
    dto.setOriginName((String) row[1]);
    dto.setOriginLat(row[2] != null ? new BigDecimal(row[2].toString()) : null);
    dto.setOriginLon(row[3] != null ? new BigDecimal(row[3].toString()) : null);
    dto.setDestStationId(toInt(row[4]));
    dto.setDestName((String) row[5]);
    dto.setDestLat(row[6] != null ? new BigDecimal(row[6].toString()) : null);
    dto.setDestLon(row[7] != null ? new BigDecimal(row[7].toString()) : null);
    dto.setEstimatedTrips(toLong(row[8]));
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
}
