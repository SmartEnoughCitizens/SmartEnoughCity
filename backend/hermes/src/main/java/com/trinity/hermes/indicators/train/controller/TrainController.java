package com.trinity.hermes.indicators.train.controller;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.train.dto.TrainDTO;
import com.trinity.hermes.indicators.train.dto.TrainDelayPatternDTO;
import com.trinity.hermes.indicators.train.dto.TrainKpiDTO;
import com.trinity.hermes.indicators.train.dto.TrainLiveDTO;
import com.trinity.hermes.indicators.train.dto.TrainServiceStatsDTO;
import com.trinity.hermes.indicators.train.dto.TrainStationUtilizationDTO;
import com.trinity.hermes.indicators.train.facade.TrainFacade;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TrainController {

  private final TrainFacade trainFacade;

  /** Station list — keeps existing URL under /api/v1/dashboard for frontend compatibility. */
  @GetMapping("/api/v1/dashboard/train")
  public ResponseEntity<Map<String, Object>> getTrainData(
      @RequestParam(defaultValue = "200") Integer limit) {

    log.info("Dashboard API: Getting train data with limit: {}", LogSanitizer.sanitizeLog(limit));

    try {
      List<TrainDTO> stations = trainFacade.getStations(limit);

      Map<String, Object> response = new HashMap<>();
      response.put("indicatorType", "train");
      response.put("totalRecords", stations.size());
      response.put("data", stations);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error fetching train data: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/api/v1/train/kpis")
  public ResponseEntity<TrainKpiDTO> getKpis() {
    log.info("GET /api/v1/train/kpis");
    return ResponseEntity.ok(trainFacade.getKpis());
  }

  @GetMapping("/api/v1/train/live-trains")
  public ResponseEntity<List<TrainLiveDTO>> getLiveTrains() {
    log.info("GET /api/v1/train/live-trains");
    return ResponseEntity.ok(trainFacade.getLiveTrains());
  }

  @GetMapping("/api/v1/train/service-stats")
  public ResponseEntity<TrainServiceStatsDTO> getServiceStats() {
    log.info("GET /api/v1/train/service-stats");
    return ResponseEntity.ok(trainFacade.getServiceStats());
  }

  /**
   * Per-station utilization for the Greater Dublin Area.
   *
   * <p>Returns stations sorted by service count (busiest first), each tagged HIGH / MEDIUM / LOW
   * relative to the Dublin mean.
   */
  @GetMapping("/api/v1/train/utilization")
  public ResponseEntity<List<TrainStationUtilizationDTO>> getStationUtilization() {
    log.info("GET /api/v1/train/utilization");
    try {
      return ResponseEntity.ok(trainFacade.getStationUtilization());
    } catch (Exception e) {
      log.error("Error fetching station utilization: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Recurring delay patterns for the Greater Dublin Area.
   *
   * <p>Aggregates historical {@code irish_rail_station_data} by station, route, train type, and
   * time-of-day bucket (MORNING_PEAK / MIDDAY / AFTERNOON / EVENING_PEAK / NIGHT). Only patterns
   * with an average delay ≥ 1 min are returned, sorted worst-first.
   *
   * @param days how many calendar days to look back (default 30; allowed: 7, 30, 90)
   */
  @GetMapping("/api/v1/train/delay-patterns")
  public ResponseEntity<List<TrainDelayPatternDTO>> getDelayPatterns(
      @RequestParam(defaultValue = "30") int days) {
    int safeDays = (days == 7 || days == 90) ? days : 30;
    log.info("GET /api/v1/train/delay-patterns?days={}", safeDays);
    try {
      return ResponseEntity.ok(trainFacade.getDelayPatterns(safeDays));
    } catch (Exception e) {
      log.error("Error fetching delay patterns: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
