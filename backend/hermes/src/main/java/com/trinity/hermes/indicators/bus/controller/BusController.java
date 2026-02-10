package com.trinity.hermes.indicators.bus.controller;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.bus.dto.BusTripUpdateDTO;
import com.trinity.hermes.indicators.bus.service.BusTripUpdateService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Disruption Management. Provides endpoints for disruption detection,
 * monitoring, and management.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BusController {

  private final BusTripUpdateService busTripUpdateService;

  /**
   * Get bus data for dashboard
   *
   * @param routeId Optional route ID filter
   * @param limit Number of records to return
   * @return Bus trip updates with statistics
   */
  @GetMapping("/bus")
  public ResponseEntity<Map<String, Object>> getBusData(
      @RequestParam(required = false) String routeId,
      @RequestParam(defaultValue = "100") Integer limit) {

    log.info(
        "Dashboard API: Getting bus data for route: {}, limit: {}",
        LogSanitizer.sanitizeLog(routeId),
        LogSanitizer.sanitizeLog(limit));

    try {
      Map<String, Object> response = new HashMap<>();

      if (routeId != null && !routeId.isEmpty()) {
        List<BusTripUpdateDTO> updates =
            busTripUpdateService.getBusTripUpdatesByRoute(routeId, limit);
        BusTripUpdateService.DelayStatistics stats =
            busTripUpdateService.getDelayStatistics(routeId);

        response.put("indicatorType", "bus");
        response.put("routeId", routeId);
        response.put("totalRecords", updates.size());
        response.put("data", updates);
        response.put("statistics", stats);
      } else {
        List<BusTripUpdateDTO> updates = busTripUpdateService.getAllBusTripUpdates(limit);
        List<String> routes = busTripUpdateService.getAllRoutes();

        response.put("indicatorType", "bus");
        response.put("totalRecords", updates.size());
        response.put("totalRoutes", routes.size());
        response.put("routes", routes);
        response.put("data", updates);
      }

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error fetching bus data: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Get all routes */
  @GetMapping("/bus/routes")
  public ResponseEntity<List<String>> getBusRoutes() {
    log.info("Dashboard API: Getting all bus routes");

    try {
      List<String> routes = busTripUpdateService.getAllRoutes();
      return ResponseEntity.ok(routes);
    } catch (Exception e) {
      log.error("Error fetching bus routes: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
