package com.trinity.hermes.dataanalyzer.controller;

import com.trinity.hermes.dataanalyzer.dto.BusTripUpdateDTO;
import com.trinity.hermes.dataanalyzer.dto.CycleStationDTO;
import com.trinity.hermes.dataanalyzer.dto.RecommendationEngineRequest;
import com.trinity.hermes.dataanalyzer.service.BusTripUpdateService;
import com.trinity.hermes.dataanalyzer.service.CycleStationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Recommendation Engine APIs
 * Provides a common endpoint for recommendation engine to fetch real indicator data
 */
@RestController
@RequestMapping("/api/v1/recommendation-engine")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RecommendationEngineController {

    private final BusTripUpdateService busTripUpdateService;
    private final CycleStationService cycleStationService;

    /**
     * Common API endpoint for recommendation engine
     * Accepts indicator type (bus, cycle, etc.) as parameter
     *
     * @param request RecommendationEngineRequest with indicator type and filters
     * @return List of data based on indicator type
     */
    @PostMapping("/indicators/query")
    public ResponseEntity<?> getIndicatorDataForRecommendation(
            @RequestBody RecommendationEngineRequest request) {

        log.info("Recommendation Engine API: Query received for indicator type: {}",
                request.getIndicatorType());

        // Validate request
        if (request.getIndicatorType() == null || request.getIndicatorType().trim().isEmpty()) {
            log.warn("Invalid request: indicator type is required");
            return ResponseEntity.badRequest().body("Indicator type is required");
        }

        try {
            String indicatorType = request.getIndicatorType().toLowerCase();

            switch (indicatorType) {
                case "bus":
                    return handleBusRequest(request);

                case "cycle":
                    return handleCycleRequest(request);

                default:
                    log.warn("Unsupported indicator type: {}", indicatorType);
                    return ResponseEntity.badRequest()
                            .body("Unsupported indicator type: " + indicatorType);
            }

        } catch (Exception e) {
            log.error("Error fetching indicator data for recommendation engine: {}",
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching data: " + e.getMessage());
        }
    }

    /**
     * Handle bus data request
     */
    private ResponseEntity<List<BusTripUpdateDTO>> handleBusRequest(RecommendationEngineRequest request) {
        log.info("Fetching bus trip updates for recommendation engine");

        Integer limit = request.getLimit() != null ? request.getLimit() : 100;

        List<BusTripUpdateDTO> data = busTripUpdateService.getAllBusTripUpdates(limit);

        log.info("Returning {} bus trip records", data.size());
        return ResponseEntity.ok(data);
    }

    /**
     * Handle cycle data request
     */
    private ResponseEntity<List<CycleStationDTO>> handleCycleRequest(RecommendationEngineRequest request) {
        log.info("Fetching cycle station data for recommendation engine");

        Integer limit = request.getLimit() != null ? request.getLimit() : 100;

        List<CycleStationDTO> data = cycleStationService.getAllCycleStations(limit);

        log.info("Returning {} cycle station records", data.size());
        return ResponseEntity.ok(data);
    }

    /**
     * GET endpoint as an alternative to POST
     * For simple queries without filters
     *
     * @param indicatorType Type of indicator (bus, cycle, etc.)
     * @param limit Number of records to return
     * @return List of data
     */
    @GetMapping("/indicators/{indicatorType}")
    public ResponseEntity<?> getIndicatorData(
            @PathVariable String indicatorType,
            @RequestParam(defaultValue = "100") Integer limit) {

        log.info("Recommendation Engine API: GET request for indicator: {} with limit: {}",
                indicatorType, limit);

        try {
            RecommendationEngineRequest request = new RecommendationEngineRequest();
            request.setIndicatorType(indicatorType);
            request.setLimit(limit);

            return getIndicatorDataForRecommendation(request);
        } catch (Exception e) {
            log.error("Error fetching indicator data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching data: " + e.getMessage());
        }
    }
}