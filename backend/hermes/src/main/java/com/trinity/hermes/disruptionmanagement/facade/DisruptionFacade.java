package com.trinity.hermes.disruptionmanagement.facade;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.disruptionmanagement.dto.*;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionRepository;
import com.trinity.hermes.disruptionmanagement.service.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Facade layer for Disruption Management Engine. Orchestrates the complete disruption response
 * workflow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DisruptionFacade {

  // Core Services
  @SuppressFBWarnings(value = "EI2", justification = "Spring-injected service dependency")
  private final DisruptionService disruptionService;
  private final ThresholdDetectionService thresholdDetectionService;

  private final com.trinity.hermes.notification.services.NotificationFacade notificationFacade;

    @SuppressFBWarnings(value = "EI2", justification = "Spring-injected service dependency")
  private final IncidentLoggingService incidentLoggingService;

  // Repository
  private final DisruptionRepository disruptionRepository;

  // TODO: Inject Recommendation Facade when needed
  // private final RecommendationFacade recommendationFacade;

  // =============================================================================
  // MAIN DISRUPTION MANAGEMENT WORKFLOW
  // =============================================================================

  /**
   * Main entry point for disruption detection from Python data handler service. This is called when
   * the Python service detects a disruption in real-time data.
   *
   * <p>Workflow: 1. Validate threshold criteria 2. Create disruption record 3. Calculate
   * alternative routes 4. Compile solution 5. Send to notification handler 6. Log incident
   *
   * @param request The disruption detection request from Python service
   * @return The compiled solution ready for notification
   */
  public DisruptionSolution handleDisruptionDetection(DisruptionDetectionRequest request) {
    log.info("=== DISRUPTION DETECTION STARTED ===");
    log.info(
        "Type: {}, Severity: {}, Area: {}",
        request.getDisruptionType(),
        request.getSeverity(),
        request.getAffectedArea());

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
      disruption.setDetectedAt(LocalDateTime.now(java.time.ZoneId.of("Europe/Dublin")));

      // Save to repository to get actual ID
      Disruption saved = disruptionRepository.save(disruption);
      disruption = saved; // Use the saved entity with real ID

      incidentLoggingService.logDisruptionDetected(disruption);

      // Step 3: Process the disruption
      disruption.setStatus("ANALYZING");
      DisruptionSolution solution = processDisruption(disruption);

      // Step 4: Send to notification handler
      disruption.setStatus("NOTIFYING");
      notificationFacade.sendDisruptionNotification(solution);
      boolean notificationSent = true;
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

    // Create dummy solution object populated for notification
    DisruptionSolution solution = new DisruptionSolution();
    solution.setDisruptionId(disruption.getId());
    solution.setDisruptionType(disruption.getDisruptionType());
    solution.setSeverity(disruption.getSeverity());
    solution.setDescription(disruption.getDescription());
    solution.setAffectedArea(disruption.getAffectedArea());
    solution.setActionSummary(
        "Immediate action required: "
            + disruption.getDisruptionType()
            + " detected in "
            + disruption.getAffectedArea());
    solution.setCalculatedAt(LocalDateTime.now(java.time.ZoneId.of("Europe/Dublin")));

    // Good dummy data for notification testing
    solution.setPrimaryRecommendation(
        "Take Luas Green Line from Stephen's Green to Sandyford (15 mins)");
    solution.setAlternativeRoutes(
        List.of(
            "Option A: Dublin Bus Route 46A - Departs in 5 mins from Stop 792",
            "Option B: Dublin Bikes - Station 12 (Earlsfort Terrace) has 5 bikes available",
            "Option C: Walk - 25 mins via Ranelagh Road"));
    solution.setSecondaryRecommendations(List.of("Check Uber availability (approx €15)"));
    solution.setStepByStepInstructions(
        List.of(
            "1. Leave the disrupted stop immediately.",
            "2. Walk 200m to Luas Stop (Stephen's Green).",
            "3. Board Green Line (Southbound).",
            "4. Alight at destination."));

    return solution;
  }

  /**
   * Update an ongoing disruption (e.g., when situation changes)
   *
   * @param disruptionId ID of the disruption
   * @param updateRequest Update information
   * @return Updated solution
   */
  public DisruptionSolution updateDisruptionStatus(
      Long disruptionId, UpdateDisruptionRequest updateRequest) {
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
    log.info("Resolving disruption ID: {}", LogSanitizer.sanitizeLog(disruptionId));

    Optional<Disruption> optionalDisruption = disruptionRepository.findById(disruptionId);

    if (optionalDisruption.isEmpty()) {
      log.warn("Disruption {} not found", LogSanitizer.sanitizeLog(disruptionId));
      return false;
    }

    Disruption disruption = optionalDisruption.get();
    disruption.setStatus("RESOLVED");
    disruption.setResolvedAt(LocalDateTime.now(java.time.ZoneId.of("Europe/Dublin")));
    disruptionRepository.save(disruption);

    // notificationCoordinationService.sendResolutionNotification(disruptionId); //
    // Removed as using NotificationFacade now
    // NotificationFacade might not have resolution method yet, keeping commented
    // out or use generic notification
    // For now, logging resolution only
    // notificationFacade.sendDisruptionNotification(resolvedSolution); // Optional:
    // if we want to notify resolution
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

    return disruptionRepository.findByStatusOrderByDetectedAtDesc("ACTIVE").stream()
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
    log.debug("Retrieving disruptions with severity: {}", LogSanitizer.sanitizeLog(severity));

    return disruptionRepository.findBySeverity(severity).stream()
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
    log.debug("Retrieving disruptions in area: {}", LogSanitizer.sanitizeLog(area));

    return disruptionRepository.findByAffectedArea(area).stream()
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

  /** Convert detection request to Disruption entity */
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
