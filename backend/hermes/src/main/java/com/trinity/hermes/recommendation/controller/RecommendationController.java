package com.trinity.hermes.recommendation.controller;

import com.trinity.hermes.recommendation.dto.RecommendationEngineRequest;
import com.trinity.hermes.indicators.bus.dto.BusTripUpdateDTO;
import com.trinity.hermes.indicators.bus.service.BusTripUpdateService;
import com.trinity.hermes.indicators.cycle.dto.CycleStationDTO;
import com.trinity.hermes.indicators.cycle.service.CycleStationService;
import com.trinity.hermes.recommendation.dto.CreateRecommendationRequest;
import com.trinity.hermes.recommendation.dto.RecommendationResponse;
import com.trinity.hermes.recommendation.dto.UpdateRecommendationRequest;
import com.trinity.hermes.recommendation.facade.RecommendationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/recommendation-engine")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationFacade recommendationFacade;

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getAllRecommendations() {
        return ResponseEntity.ok(recommendationFacade.getAllRecommendations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecommendationResponse> getRecommendationById(@PathVariable Long id) {
        Optional<RecommendationResponse> recommendation = recommendationFacade.getRecommendationById(id);
        return recommendation.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RecommendationResponse> createRecommendation(
            @RequestBody CreateRecommendationRequest request) {
        RecommendationResponse created = recommendationFacade.createRecommendation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

//    @PutMapping("/{id}")
//    public ResponseEntity<RecommendationResponse> updateRecommendation(@PathVariable Long id,
//            @RequestBody UpdateRecommendationRequest request) {
//        Optional<RecommendationResponse> updated = recommendationFacade.updateRecommendation(id, request);
//        return updated.map(ResponseEntity::ok)
//                .orElseGet(() -> ResponseEntity.notFound().build());
//    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecommendation(@PathVariable Long id) {
        boolean deleted = recommendationFacade.deleteRecommendation(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

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
            String indicatorType = request.getIndicatorType().toLowerCase(java.util.Locale.ROOT);

            return switch (indicatorType) {
                case "bus" -> handleBusRequest(request);
                case "cycle" -> handleCycleRequest(request);
                default -> {
                    log.warn("Unsupported indicator type: {}", indicatorType);
                    yield ResponseEntity.badRequest()
                            .body("Unsupported indicator type: " + indicatorType);
                }
            };

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
