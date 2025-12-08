package com.trinity.hermes.dataanalyzer.controller;

import com.trinity.hermes.dataanalyzer.dto.BusTripUpdateDTO;
import com.trinity.hermes.dataanalyzer.dto.CycleStationDTO;
import com.trinity.hermes.dataanalyzer.service.BusTripUpdateService;
import com.trinity.hermes.dataanalyzer.service.CycleStationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Dashboard APIs
 * Provides real data to the frontend dashboard
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DashboardController {

    private final BusTripUpdateService busTripUpdateService;
    private final CycleStationService cycleStationService;

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

        log.info("Dashboard API: Getting bus data for route: {}, limit: {}", routeId, limit);

        try {
            Map<String, Object> response = new HashMap<>();

            if (routeId != null && !routeId.isEmpty()) {
                List<BusTripUpdateDTO> updates = busTripUpdateService.getBusTripUpdatesByRoute(routeId, limit);
                BusTripUpdateService.DelayStatistics stats = busTripUpdateService.getDelayStatistics(routeId);

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

    /**
     * Get cycle station data for dashboard
     *
     * @param limit Number of records to return
     * @return Cycle station data with statistics
     */
    @GetMapping("/cycle")
    public ResponseEntity<Map<String, Object>> getCycleData(
            @RequestParam(defaultValue = "100") Integer limit) {

        log.info("Dashboard API: Getting cycle data with limit: {}", limit);

        try {
            List<CycleStationDTO> stations = cycleStationService.getAllCycleStations(limit);
            CycleStationService.CycleStatistics stats = cycleStationService.getCycleStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("indicatorType", "cycle");
            response.put("totalRecords", stations.size());
            response.put("data", stations);
            response.put("statistics", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching cycle data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get stations with available bikes
     */
    @GetMapping("/cycle/available-bikes")
    public ResponseEntity<List<CycleStationDTO>> getAvailableBikes() {
        log.info("Dashboard API: Getting stations with available bikes");

        try {
            List<CycleStationDTO> stations = cycleStationService.getAvailableBikeStations();
            return ResponseEntity.ok(stations);
        } catch (Exception e) {
            log.error("Error fetching available bikes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get stations with available docks
     */
    @GetMapping("/cycle/available-docks")
    public ResponseEntity<List<CycleStationDTO>> getAvailableDocks() {
        log.info("Dashboard API: Getting stations with available docks");

        try {
            List<CycleStationDTO> stations = cycleStationService.getAvailableDockStations();
            return ResponseEntity.ok(stations);
        } catch (Exception e) {
            log.error("Error fetching available docks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all routes
     */
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

    /**
     * Get available indicator types
     */
    @GetMapping("/indicators/types")
    public ResponseEntity<List<String>> getAvailableIndicatorTypes() {
        log.info("Dashboard API: Getting available indicator types");

        List<String> types = List.of("bus", "cycle", "luas", "train");
        return ResponseEntity.ok(types);
    }
}