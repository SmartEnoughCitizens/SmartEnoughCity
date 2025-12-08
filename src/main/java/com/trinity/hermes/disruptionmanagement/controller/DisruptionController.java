package com.trinity.hermes.disruptionmanagement.controller;

import com.trinity.hermes.disruptionmanagement.dto.*;
import com.trinity.hermes.disruptionmanagement.facade.DisruptionFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Disruption Management.
 * Provides endpoints for disruption detection, monitoring, and management.
 */
@RestController
@RequestMapping("/api/v1/disruptions")
@RequiredArgsConstructor
@Slf4j
public class DisruptionController {

    private final DisruptionFacade disruptionFacade;

    // =============================================================================
    // DISRUPTION DETECTION ENDPOINT (Called by Python Data Handler Service)
    // =============================================================================

    /**
     * Main endpoint for disruption detection from Python data handler service.
     * This is called when the Python service detects a disruption in real-time
     * data.
     * 
     * POST /api/v1/disruptions/detect
     * 
     * @param request Disruption detection request from Python service
     * @return Compiled solution ready for notification
     */
    @PostMapping("/detect")
    public ResponseEntity<DisruptionSolution> detectDisruption(@RequestBody DisruptionDetectionRequest request) {
        log.info("Received disruption detection request from Python service: {}", request.getDisruptionType());

        try {
            DisruptionSolution solution = disruptionFacade.handleDisruptionDetection(request);

            if (solution == null) {
                // Disruption did not meet threshold criteria
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(solution);

        } catch (Exception e) {
            log.error("Error processing disruption detection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =============================================================================
    // DISRUPTION MANAGEMENT ENDPOINTS
    // =============================================================================

    /**
     * Get all active disruptions
     * GET /api/v1/disruptions/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<DisruptionResponse>> getActiveDisruptions() {
        List<DisruptionResponse> activeDisruptions = disruptionFacade.getActiveDisruptions();
        return ResponseEntity.ok(activeDisruptions);
    }

    /**
     * Get disruptions by severity
     * GET /api/v1/disruptions/severity/{severity}
     */
    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<DisruptionResponse>> getDisruptionsBySeverity(@PathVariable String severity) {
        List<DisruptionResponse> disruptions = disruptionFacade.getDisruptionsBySeverity(severity);
        return ResponseEntity.ok(disruptions);
    }

    /**
     * Get disruptions by area
     * GET /api/v1/disruptions/area/{area}
     */
    @GetMapping("/area/{area}")
    public ResponseEntity<List<DisruptionResponse>> getDisruptionsByArea(@PathVariable String area) {
        List<DisruptionResponse> disruptions = disruptionFacade.getDisruptionsByArea(area);
        return ResponseEntity.ok(disruptions);
    }

    /**
     * Resolve a disruption (mark as no longer active)
     * POST /api/v1/disruptions/{id}/resolve
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<Void> resolveDisruption(@PathVariable Long id) {
        boolean resolved = disruptionFacade.resolveDisruption(id);
        return resolved ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Get incident logs for a disruption
     * GET /api/v1/disruptions/{id}/logs
     */
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<String>> getDisruptionLogs(@PathVariable Long id) {
        List<String> logs = disruptionFacade.getDisruptionLogs(id);
        return ResponseEntity.ok(logs);
    }

    // =============================================================================
    // STANDARD CRUD OPERATIONS
    // =============================================================================

    @GetMapping
    public ResponseEntity<List<DisruptionResponse>> getAllDisruptions() {
        return ResponseEntity.ok(disruptionFacade.getAllDisruptions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisruptionResponse> getDisruptionById(@PathVariable Long id) {
        Optional<DisruptionResponse> disruption = disruptionFacade.getDisruptionById(id);
        return disruption.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DisruptionResponse> createDisruption(@RequestBody CreateDisruptionRequest request) {
        DisruptionResponse created = disruptionFacade.createDisruption(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DisruptionResponse> updateDisruption(@PathVariable Long id,
            @RequestBody UpdateDisruptionRequest request) {
        Optional<DisruptionResponse> updated = disruptionFacade.updateDisruption(id, request);
        return updated.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDisruption(@PathVariable Long id) {
        boolean deleted = disruptionFacade.deleteDisruption(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
