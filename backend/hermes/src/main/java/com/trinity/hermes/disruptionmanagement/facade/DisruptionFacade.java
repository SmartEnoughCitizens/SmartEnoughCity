package com.trinity.hermes.disruptionmanagement.facade;

import com.trinity.hermes.disruptionmanagement.dto.*;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionRepository;
import com.trinity.hermes.disruptionmanagement.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Facade layer for Disruption Management Engine.
 * Orchestrates the complete disruption response workflow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DisruptionFacade {

    // Core Services
    private final DisruptionService disruptionService;
    private final ThresholdDetectionService thresholdDetectionService;

    private final NotificationCoordinationService notificationCoordinationService;
    private final IncidentLoggingService incidentLoggingService;

    // Repository
    private final DisruptionRepository disruptionRepository;

    // TODO: Inject Recommendation Facade when needed
    // private final RecommendationFacade recommendationFacade;

    // =============================================================================
    // MAIN DISRUPTION MANAGEMENT WORKFLOW
    // =============================================================================

    /**
     * Main entry point for disruption detection from Python data handler service.
     * This is called when the Python service detects a disruption in real-time
     * data.
     * 
     * Workflow:
     * 1. Validate threshold criteria
     * 2. Create disruption record
     * 3. Calculate alternative routes
     * 4. Compile solution
     * 5. Send to notification handler
     * 6. Log incident
     * 
     * @param request The disruption detection request from Python service
     * @return The compiled solution ready for notification
     */
    public DisruptionSolution handleDisruptionDetection(DisruptionDetectionRequest request) {
        log.info("=== DISRUPTION DETECTION STARTED ===");
        log.info("Type: {}, Severity: {}, Area: {}",
                request.getDisruptionType(), request.getSeverity(), request.getAffectedArea());

        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Validate threshold
            if (!thresholdDetectionService.meetsThreshold(request)) {
                log.info("Disruption does not meet threshold criteria. Ignoring.");
                return null;
            }

            // Step 2: Create disruption entity
            Disruption disruption = createDisruptionFromDetection(request);
            disruption.setStatus("DETECTED");
            disruption.setDetectedAt(LocalDateTime.now());

            // Save to repository to get actual ID
            Disruption saved = disruptionRepository.save(disruption);
            disruption = saved; // Use the saved entity with real ID

            incidentLoggingService.logDisruptionDetected(disruption);

            // Step 3: Process the disruption
            disruption.setStatus("ANALYZING");
            DisruptionSolution solution = processDisruption(disruption);

            // Step 4: Send to notification handler
            disruption.setStatus("NOTIFYING");
            boolean notificationSent = notificationCoordinationService.sendDisruptionNotifications(solution);
            disruption.setNotificationSent(notificationSent);

            // Step 5: Update status
            if (notificationSent) {
                disruption.setStatus("ACTIVE");
                log.info("Disruption processing completed successfully");
            }

            long endTime = System.currentTimeMillis();
            incidentLoggingService.logPerformanceMetrics(
                    disruption.getId(),
                    0L, // Detection latency (would be calculated from data source timestamp)
                    endTime - startTime);

            log.info("=== DISRUPTION DETECTION COMPLETED in {}ms ===", endTime - startTime);
            return solution;

        } catch (Exception e) {
            log.error("Error processing disruption detection", e);
            throw new RuntimeException("Failed to process disruption", e);
        }
    }

    /**
     * Process a disruption by calculating routes and compiling solutions
     * 
     * @param disruption The disruption to process
     * @return Compiled solution
     */
    public DisruptionSolution processDisruption(Disruption disruption) {
        log.info("Processing disruption ID: {}", disruption.getId());

        return new DisruptionSolution();
    }

    /**
     * Update an ongoing disruption (e.g., when situation changes)
     * 
     * @param disruptionId  ID of the disruption
     * @param updateRequest Update information
     * @return Updated solution
     */
    public DisruptionSolution updateDisruptionStatus(Long disruptionId, UpdateDisruptionRequest updateRequest) {
        log.info("Updating disruption ID: {}", disruptionId);

        // TODO: Implement update logic
        // 1. Retrieve existing disruption
        // 2. Update fields
        // 3. Recalculate if necessary
        // 4. Send update notification

        return null; // Placeholder
    }

    /**
     * Resolve a disruption when it's no longer active
     * 
     * @param disruptionId ID of the disruption
     * @return true if resolved successfully
     */
    public boolean resolveDisruption(Long disruptionId) {
        log.info("Resolving disruption ID: {}", disruptionId);

        Optional<Disruption> optionalDisruption = disruptionRepository.findById(disruptionId);

        if (optionalDisruption.isEmpty()) {
            log.warn("Disruption {} not found", disruptionId);
            return false;
        }

        Disruption disruption = optionalDisruption.get();
        disruption.setStatus("RESOLVED");
        disruption.setResolvedAt(LocalDateTime.now());
        disruptionRepository.save(disruption);

        notificationCoordinationService.sendResolutionNotification(disruptionId);
        incidentLoggingService.logDisruptionResolved(disruptionId, "Normal service resumed");

        return true;
    }

    // =============================================================================
    // STANDARD CRUD OPERATIONS (For manual disruption management)
    // =============================================================================

    public List<DisruptionResponse> getAllDisruptions() {
        return disruptionService.getAllDisruptions();
    }

    public Optional<DisruptionResponse> getDisruptionById(Long id) {
        return disruptionService.getDisruptionById(id);
    }

    public DisruptionResponse createDisruption(CreateDisruptionRequest request) {
        return disruptionService.createDisruption(request);
    }

    public Optional<DisruptionResponse> updateDisruption(Long id, UpdateDisruptionRequest request) {
        return disruptionService.updateDisruption(id, request);
    }

    public boolean deleteDisruption(Long id) {
        return disruptionService.deleteDisruption(id);
    }

    // =============================================================================
    // QUERY AND MONITORING OPERATIONS
    // =============================================================================

    /**
     * Get all active disruptions
     * 
     * @return List of active disruptions
     */
    public List<DisruptionResponse> getActiveDisruptions() {
        log.debug("Retrieving active disruptions");

        return disruptionRepository.findByStatusOrderByDetectedAtDesc("ACTIVE")
                .stream()
                .map(disruptionService::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get disruptions by severity
     * 
     * @param severity Severity level to filter by
     * @return List of disruptions matching severity
     */
    public List<DisruptionResponse> getDisruptionsBySeverity(String severity) {
        log.debug("Retrieving disruptions with severity: {}", severity);

        return disruptionRepository.findBySeverity(severity)
                .stream()
                .map(disruptionService::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get disruptions affecting a specific area
     * 
     * @param area Area to check
     * @return List of disruptions in that area
     */
    public List<DisruptionResponse> getDisruptionsByArea(String area) {
        log.debug("Retrieving disruptions in area: {}", area);

        return disruptionRepository.findByAffectedArea(area)
                .stream()
                .map(disruptionService::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get incident logs for a disruption
     * 
     * @param disruptionId ID of the disruption
     * @return List of log entries
     */
    public List<String> getDisruptionLogs(Long disruptionId) {
        return incidentLoggingService.getIncidentLogs(disruptionId);
    }

    // =============================================================================
    // HELPER METHODS
    // =============================================================================

    /**
     * Convert detection request to Disruption entity
     */
    private Disruption createDisruptionFromDetection(DisruptionDetectionRequest request) {
        Disruption disruption = new Disruption();

        // Basic information
        disruption.setName(request.getDisruptionType() + " - " + request.getAffectedArea());
        disruption.setDescription(request.getDescription());
        disruption.setDisruptionType(request.getDisruptionType());
        disruption.setSeverity(request.getSeverity());

        // Location
        disruption.setLatitude(request.getLatitude());
        disruption.setLongitude(request.getLongitude());
        disruption.setAffectedArea(request.getAffectedArea());

        // Transport information
        disruption.setAffectedTransportModes(request.getAffectedTransportModes());
        disruption.setAffectedRoutes(request.getAffectedRoutes());
        disruption.setAffectedStops(request.getAffectedStops());

        // Timing
        disruption.setStartTime(request.getEstimatedStartTime());
        disruption.setEstimatedEndTime(request.getEstimatedEndTime());
        disruption.setDelayMinutes(request.getDelayMinutes());

        // Source
        disruption.setDataSource(request.getDataSource());
        disruption.setSourceReferenceId(request.getSourceReferenceId());

        // Additional context
        disruption.setEventDetails(request.getEventName());
        disruption.setConstructionDetails(request.getConstructionProject());

        return disruption;
    }
}
