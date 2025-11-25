package com.trinity.hermes.dataanalyzer.controller;


import com.trinity.hermes.dataanalyzer.dto.IndicatorDTO;
import com.trinity.hermes.dataanalyzer.dto.RecommendationEngineRequest;
import com.trinity.hermes.dataanalyzer.service.IndicatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Recommendation Engine APIs
 * Provides a common endpoint for recommendation engine to fetch indicator data
 */
@RestController
@RequestMapping("/api/v1/recommendation-engine")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Configure appropriately for production
public class RecommendationEngineController {

    private final IndicatorService indicatorService;

    /**
     * Common API endpoint for recommendation engine
     * Accepts indicator type (bus, car, etc.) as parameter
     *
     * @param request RecommendationEngineRequest with indicator type and filters
     * @return List of IndicatorDTO
     */
    @PostMapping("/indicators/query")
    public ResponseEntity<List<IndicatorDTO>> getIndicatorDataForRecommendation(
            @RequestBody RecommendationEngineRequest request) {

        log.info("Recommendation Engine API: Query received for indicator type: {}",
                request.getIndicatorType());

        // Validate request
        if (request.getIndicatorType() == null || request.getIndicatorType().trim().isEmpty()) {
            log.warn("Invalid request: indicator type is required");
            return ResponseEntity.badRequest().build();
        }

        try {
            List<IndicatorDTO> indicators = indicatorService.getIndicatorDataForRecommendation(request);

            log.info("Recommendation Engine API: Returning {} records for indicator: {}",
                    indicators.size(), request.getIndicatorType());

            return ResponseEntity.ok(indicators);
        } catch (Exception e) {
            log.error("Error fetching indicator data for recommendation engine: {}",
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET endpoint as an alternative to POST
     * For simple queries without date filters
     *
     * @param indicatorType Type of indicator (bus, car, etc.)
     * @param limit Number of records to return
     * @return List of IndicatorDTO
     */
    @GetMapping("/indicators/{indicatorType}")
    public ResponseEntity<List<IndicatorDTO>> getIndicatorData(
            @PathVariable String indicatorType,
            @RequestParam(defaultValue = "100") Integer limit) {

        log.info("Recommendation Engine API: GET request for indicator: {} with limit: {}",
                indicatorType, limit);

        try {
            RecommendationEngineRequest request = new RecommendationEngineRequest();
            request.setIndicatorType(indicatorType);
            request.setLimit(limit);

            List<IndicatorDTO> indicators = indicatorService.getIndicatorDataForRecommendation(request);
            return ResponseEntity.ok(indicators);
        } catch (Exception e) {
            log.error("Error fetching indicator data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}