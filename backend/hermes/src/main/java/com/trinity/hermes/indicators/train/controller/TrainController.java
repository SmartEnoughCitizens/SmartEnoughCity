package com.trinity.hermes.indicators.train.controller;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.train.dto.TrainDTO;
import com.trinity.hermes.indicators.train.dto.TrainKpiDTO;
import com.trinity.hermes.indicators.train.dto.TrainLiveDTO;
import com.trinity.hermes.indicators.train.dto.TrainServiceStatsDTO;
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

  /**
   * Station list — keeps existing URL under /api/v1/dashboard for frontend
   * compatibility.
   */
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
}
