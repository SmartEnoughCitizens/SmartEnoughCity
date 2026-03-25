package com.trinity.hermes.indicators.tram.controller;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.tram.dto.*;
import com.trinity.hermes.indicators.tram.facade.TramFacade;
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
public class TramController {

  private final TramFacade tramFacade;

  /** Station list — under /api/v1/dashboard for frontend compatibility. */
  @GetMapping("/api/v1/dashboard/tram")
  public ResponseEntity<Map<String, Object>> getTramData(
      @RequestParam(defaultValue = "200") Integer limit) {

    log.info("Dashboard API: Getting tram data with limit: {}", LogSanitizer.sanitizeLog(limit));

    try {
      List<TramStopDTO> stops = tramFacade.getStops(limit);

      Map<String, Object> response = new HashMap<>();
      response.put("indicatorType", "tram");
      response.put("totalRecords", stops.size());
      response.put("data", stops);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error fetching tram data: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/api/v1/tram/kpis")
  public ResponseEntity<TramKpiDTO> getKpis() {
    log.info("GET /api/v1/tram/kpis");
    return ResponseEntity.ok(tramFacade.getKpis());
  }

  @GetMapping("/api/v1/tram/live-forecasts")
  public ResponseEntity<List<TramLiveForecastDTO>> getLiveForecasts() {
    log.info("GET /api/v1/tram/live-forecasts");
    return ResponseEntity.ok(tramFacade.getLiveForecasts());
  }

  @GetMapping("/api/v1/tram/delays")
  public ResponseEntity<List<TramDelayDTO>> getDelays() {
    log.info("GET /api/v1/tram/delays");
    return ResponseEntity.ok(tramFacade.getDelays());
  }

  @GetMapping("/api/v1/tram/hourly-distribution")
  public ResponseEntity<List<TramHourlyDistributionDTO>> getHourlyDistribution() {
    log.info("GET /api/v1/tram/hourly-distribution");
    return ResponseEntity.ok(tramFacade.getHourlyDistribution());
  }
}
