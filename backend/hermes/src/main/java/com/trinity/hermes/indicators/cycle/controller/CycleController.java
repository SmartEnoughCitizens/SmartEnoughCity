package com.trinity.hermes.indicators.cycle.controller;


import com.trinity.hermes.indicators.cycle.dto.CycleStationDTO;
import com.trinity.hermes.indicators.cycle.service.CycleStationService;
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
public class CycleController {

    private final CycleStationService cycleStationService;

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
}
