package com.trinity.hermes.dataanalyzer.controller;



import com.trinity.hermes.dataanalyzer.dto.IndicatorDTO;
import com.trinity.hermes.dataanalyzer.dto.IndicatorResponse;
import com.trinity.hermes.dataanalyzer.service.IndicatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Dashboard APIs
 * Provides indicator data to the frontend dashboard
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Configure appropriately for production
public class DashboardController {

    private final IndicatorService indicatorService;

    /**
     * Get indicator data for a specific type (e.g., bus, car)
     * Endpoint for dashboard visualization
     *
     * @param indicatorType Type of indicator (bus, car, energy, water, etc.)
     * @return IndicatorResponse with data and statistics
     */
    @GetMapping("/indicators/{indicatorType}")
    public ResponseEntity<IndicatorResponse> getIndicatorData(
            @PathVariable String indicatorType) {

        log.info("Dashboard API: Getting indicator data for type: {}", indicatorType);

        try {
            IndicatorResponse response = indicatorService.getIndicatorDataForDashboard(indicatorType);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching indicator data for dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

//    /**
//     * Get latest N records for a specific indicator type
//     *
//     * @param indicatorType Type of indicator
//     * @param limit Number of records to return (default: 10)
//     * @return List of IndicatorDTO
//     */
//    @GetMapping("/indicators/{indicatorType}/latest")
//    public ResponseEntity<List<IndicatorDTO>> getLatestIndicatorData(
//            @PathVariable String indicatorType,
//            @RequestParam(defaultValue = "10") int limit) {
//
//        log.info("Dashboard API: Getting latest {} records for indicator: {}", limit, indicatorType);
//
//        try {
//            List<IndicatorDTO> indicators = indicatorService.getLatestIndicatorData(indicatorType, limit);
//            return ResponseEntity.ok(indicators);
//        } catch (Exception e) {
//            log.error("Error fetching latest indicator data: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }

    /**
     * Get all available indicator types
     * This would typically come from a configuration or database
     */
    @GetMapping("/indicators/types")
    public ResponseEntity<List<String>> getAvailableIndicatorTypes() {
        log.info("Dashboard API: Getting available indicator types");

        List<String> types = List.of("bus", "car");
        return ResponseEntity.ok(types);
    }
}