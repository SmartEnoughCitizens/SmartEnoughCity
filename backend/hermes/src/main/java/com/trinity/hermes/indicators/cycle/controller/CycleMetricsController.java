package com.trinity.hermes.indicators.cycle.controller;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.cycle.dto.NetworkKpiDTO;
import com.trinity.hermes.indicators.cycle.dto.NetworkSummaryDTO;
import com.trinity.hermes.indicators.cycle.dto.RegionMetricsDTO;
import com.trinity.hermes.indicators.cycle.dto.StationLiveDTO;
import com.trinity.hermes.indicators.cycle.dto.StationODPairDTO;
import com.trinity.hermes.indicators.cycle.dto.StationRankingDTO;
import com.trinity.hermes.indicators.cycle.dto.StationTimeSeriesDTO;
import com.trinity.hermes.indicators.cycle.service.CycleMetricsService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comprehensive cycle (Dublin Bikes) metrics controller. Covers live data, historical statistics,
 * rankings, and derived KPIs.
 */
@RestController
@RequestMapping("/api/v1/cycle")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CycleMetricsController {

  private final CycleMetricsService cycleMetricsService;

  // -------------------------------------------------------------------------
  // Live Station Data
  // -------------------------------------------------------------------------

  /**
   * All stations with their latest snapshot data, fullness %, and status colour. Suitable for the
   * map view and station list.
   */
  @GetMapping("/stations/live")
  public ResponseEntity<List<StationLiveDTO>> getLiveStations() {
    log.info("GET /api/v1/cycle/stations/live");
    try {
      return ResponseEntity.ok(cycleMetricsService.getLiveStations());
    } catch (Exception e) {
      log.error("Error fetching live stations: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Network-level summary: total bikes, docks, empty/full counts, avg fullness. */
  @GetMapping("/network/summary")
  public ResponseEntity<NetworkSummaryDTO> getNetworkSummary() {
    log.info("GET /api/v1/cycle/network/summary");
    try {
      return ResponseEntity.ok(cycleMetricsService.getNetworkSummary());
    } catch (Exception e) {
      log.error("Error fetching network summary: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Region-level aggregations: station count, capacity, avg usage, empty/full counts. */
  @GetMapping("/regions")
  public ResponseEntity<List<RegionMetricsDTO>> getRegionMetrics() {
    log.info("GET /api/v1/cycle/regions");
    try {
      return ResponseEntity.ok(cycleMetricsService.getRegionMetrics());
    } catch (Exception e) {
      log.error("Error fetching region metrics: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // -------------------------------------------------------------------------
  // Historical Time-series
  // -------------------------------------------------------------------------

  /**
   * Time-series data for a single station. granularity: hour | day (default) | week from / to:
   * ISO-8601 instants (defaults to last 7 days)
   */
  @GetMapping("/stations/{stationId}/history")
  public ResponseEntity<List<StationTimeSeriesDTO>> getStationHistory(
      @PathVariable Integer stationId,
      @RequestParam(defaultValue = "day") String granularity,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {

    log.info(
        "GET /api/v1/cycle/stations/{}/history granularity={}",
        LogSanitizer.sanitizeLog(stationId),
        LogSanitizer.sanitizeLog(granularity));

    Instant resolvedFrom = from != null ? from : Instant.now().minus(7, ChronoUnit.DAYS);
    Instant resolvedTo = to != null ? to : Instant.now();

    try {
      return ResponseEntity.ok(
          cycleMetricsService.getStationTimeSeries(
              stationId, granularity, resolvedFrom, resolvedTo));
    } catch (Exception e) {
      log.error("Error fetching station history: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // -------------------------------------------------------------------------
  // Network Trends
  // -------------------------------------------------------------------------

  /** Hourly usage profile across the whole network (0–23). days: lookback window (default 30). */
  @GetMapping("/trends/hourly")
  public ResponseEntity<Map<Integer, Double>> getHourlyProfile(
      @RequestParam(defaultValue = "30") int days) {
    log.info("GET /api/v1/cycle/trends/hourly days={}", LogSanitizer.sanitizeLog(days));
    try {
      return ResponseEntity.ok(cycleMetricsService.getHourlyUsageProfile(days));
    } catch (Exception e) {
      log.error("Error fetching hourly profile: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Day-of-week usage profile across the network (0=Sunday … 6=Saturday). days: lookback window
   * (default 90).
   */
  @GetMapping("/trends/weekly")
  public ResponseEntity<Map<Integer, Double>> getWeeklyProfile(
      @RequestParam(defaultValue = "90") int days) {
    log.info("GET /api/v1/cycle/trends/weekly days={}", LogSanitizer.sanitizeLog(days));
    try {
      return ResponseEntity.ok(cycleMetricsService.getWeeklyUsageProfile(days));
    } catch (Exception e) {
      log.error("Error fetching weekly profile: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Weekday vs weekend average usage comparison. days: lookback window (default 90). */
  @GetMapping("/trends/weekday-vs-weekend")
  public ResponseEntity<Map<String, Double>> getWeekdayVsWeekend(
      @RequestParam(defaultValue = "90") int days) {
    log.info("GET /api/v1/cycle/trends/weekday-vs-weekend days={}", LogSanitizer.sanitizeLog(days));
    try {
      return ResponseEntity.ok(cycleMetricsService.getWeekdayVsWeekendUsage(days));
    } catch (Exception e) {
      log.error("Error fetching weekday vs weekend: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Daily network trend. days: lookback window (default 30). */
  @GetMapping("/trends/daily")
  public ResponseEntity<List<StationTimeSeriesDTO>> getDailyTrend(
      @RequestParam(defaultValue = "30") int days) {
    log.info("GET /api/v1/cycle/trends/daily days={}", LogSanitizer.sanitizeLog(days));
    try {
      return ResponseEntity.ok(cycleMetricsService.getNetworkDailyTrend(days));
    } catch (Exception e) {
      log.error("Error fetching daily trend: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Monthly network trend. months: lookback window (default 12). */
  @GetMapping("/trends/monthly")
  public ResponseEntity<List<StationTimeSeriesDTO>> getMonthlyTrend(
      @RequestParam(defaultValue = "12") int months) {
    log.info("GET /api/v1/cycle/trends/monthly months={}", LogSanitizer.sanitizeLog(months));
    try {
      return ResponseEntity.ok(cycleMetricsService.getNetworkMonthlyTrend(months));
    } catch (Exception e) {
      log.error("Error fetching monthly trend: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // -------------------------------------------------------------------------
  // Station Rankings
  // -------------------------------------------------------------------------

  /** Top N busiest stations by avg usage rate. days: lookback, limit: max results. */
  @GetMapping("/rankings/busiest")
  public ResponseEntity<List<StationRankingDTO>> getBusiestStations(
      @RequestParam(defaultValue = "7") int days, @RequestParam(defaultValue = "10") int limit) {
    log.info(
        "GET /api/v1/cycle/rankings/busiest days={} limit={}",
        LogSanitizer.sanitizeLog(days),
        LogSanitizer.sanitizeLog(limit));
    try {
      return ResponseEntity.ok(cycleMetricsService.getBusiestStations(days, limit));
    } catch (Exception e) {
      log.error("Error fetching busiest stations: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Top N least used stations by avg usage rate. days: lookback, limit: max results. */
  @GetMapping("/rankings/underused")
  public ResponseEntity<List<StationRankingDTO>> getLeastUsedStations(
      @RequestParam(defaultValue = "7") int days, @RequestParam(defaultValue = "10") int limit) {
    log.info(
        "GET /api/v1/cycle/rankings/underused days={} limit={}",
        LogSanitizer.sanitizeLog(days),
        LogSanitizer.sanitizeLog(limit));
    try {
      return ResponseEntity.ok(cycleMetricsService.getLeastUsedStations(days, limit));
    } catch (Exception e) {
      log.error("Error fetching least used stations: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // -------------------------------------------------------------------------
  // Origin-Destination Heatmap
  // -------------------------------------------------------------------------

  /**
   * Top N inferred origin-destination pairs based on correlated departures and arrivals within a
   * 30-minute window. Data covers the previous full calendar month. limit: max pairs returned
   * (default 50).
   */
  @GetMapping("/od/heatmap")
  public ResponseEntity<List<StationODPairDTO>> getODHeatmap(
      @RequestParam(defaultValue = "50") int limit) {
    log.info("GET /api/v1/cycle/od/heatmap limit={}", LogSanitizer.sanitizeLog(limit));
    try {
      return ResponseEntity.ok(cycleMetricsService.getODHeatmap(limit));
    } catch (Exception e) {
      log.error("Error fetching OD heatmap: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // -------------------------------------------------------------------------
  // Network KPIs
  // -------------------------------------------------------------------------

  /**
   * Comprehensive network KPIs: rebalancing need, imbalance score, turnover rate, daily trip
   * estimate, weekday vs weekend usage, hourly profile, 30-day trend.
   */
  @GetMapping("/network/kpi")
  public ResponseEntity<NetworkKpiDTO> getNetworkKpi() {
    log.info("GET /api/v1/cycle/network/kpi");
    try {
      return ResponseEntity.ok(cycleMetricsService.getNetworkKpi());
    } catch (Exception e) {
      log.error("Error fetching network KPIs: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
