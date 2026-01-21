package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.dto.DisruptionDetectionRequest;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for monitoring data streams and detecting disruption thresholds. Works in
 * conjunction with the Python data handler service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdDetectionService {

  // Threshold Constants
  private static final int CRITICAL_DELAY_THRESHOLD = 30; // minutes
  private static final int HIGH_DELAY_THRESHOLD = 20; // minutes
  private static final int MEDIUM_DELAY_THRESHOLD = 10; // minutes

  private static final int CRITICAL_ROUTE_COUNT = 5;
  private static final int HIGH_ROUTE_COUNT = 3;
  private static final int MEDIUM_ROUTE_COUNT = 1;

  private static final List<String> MAJOR_TRANSPORT_HUBS =
      List.of("Central Station", "Airport", "Main Terminal", "City Center");

  /**
   * Validate if the detected disruption meets threshold criteria
   *
   * @param request The disruption detection request
   * @return true if threshold is met and disruption should be processed
   */
  public boolean meetsThreshold(DisruptionDetectionRequest request) {
    log.info(
        "Checking threshold for disruption type: {}, severity: {}",
        request.getDisruptionType(),
        request.getSeverity());

    // Check 1: Minimum delay threshold
    Integer delayMinutes = request.getDelayMinutes();
    if (delayMinutes != null && delayMinutes >= MEDIUM_DELAY_THRESHOLD) {
      log.info(
          "✓ Threshold met: Delay {} minutes exceeds minimum {}",
          delayMinutes,
          MEDIUM_DELAY_THRESHOLD);
      return true;
    }

    // Check 2: Number of affected routes
    List<String> affectedRoutes = request.getAffectedRoutes();
    if (affectedRoutes != null && affectedRoutes.size() >= MEDIUM_ROUTE_COUNT) {
      log.info("✓ Threshold met: {} routes affected", affectedRoutes.size());
      return true;
    }

    // Check 3: Severity level (minimum MEDIUM)
    String severity = request.getSeverity();
    if (severity != null && !severity.equalsIgnoreCase("LOW")) {
      log.info("✓ Threshold met: Severity {} is actionable", severity);
      return true;
    }

    // Check 4: Major transport hub affected
    String affectedArea = request.getAffectedArea();
    List<String> affectedStops = request.getAffectedStops();
    if (affectedArea != null && isMajorHub(affectedArea)) {
      log.info("✓ Threshold met: Major hub affected: {}", affectedArea);
      return true;
    }
    if (affectedStops != null && affectedStops.stream().anyMatch(this::isMajorHub)) {
      log.info("✓ Threshold met: Major hub in affected stops");
      return true;
    }

    // Check 5: Service cancellation (always actionable)
    if ("CANCELLATION".equalsIgnoreCase(request.getDisruptionType())) {
      log.info("✓ Threshold met: Service cancellation detected");
      return true;
    }

    log.info("✗ Threshold not met - disruption will be ignored");
    return false;
  }

  /**
   * Calculate severity level based on disruption metrics
   *
   * @param disruption The disruption to analyze
   * @return Calculated severity (LOW, MEDIUM, HIGH, CRITICAL)
   */
  public String calculateSeverity(Disruption disruption) {
    log.debug("Calculating severity for disruption ID: {}", disruption.getId());

    int severityScore = 0;

    // Factor 1: Delay duration (0-40 points)
    Integer delayMinutes = disruption.getDelayMinutes();
    if (delayMinutes != null) {
      if (delayMinutes >= CRITICAL_DELAY_THRESHOLD) {
        severityScore += 40;
      } else if (delayMinutes >= HIGH_DELAY_THRESHOLD) {
        severityScore += 30;
      } else if (delayMinutes >= MEDIUM_DELAY_THRESHOLD) {
        severityScore += 20;
      } else {
        severityScore += 10;
      }
    }

    // Factor 2: Number of affected routes (0-30 points)
    List<String> affectedRoutes = disruption.getAffectedRoutes();
    if (affectedRoutes != null) {
      int routeCount = affectedRoutes.size();
      if (routeCount >= CRITICAL_ROUTE_COUNT) {
        severityScore += 30;
      } else if (routeCount >= HIGH_ROUTE_COUNT) {
        severityScore += 20;
      } else if (routeCount >= MEDIUM_ROUTE_COUNT) {
        severityScore += 10;
      }
    }

    // Factor 3: Major hub affected (0-20 points)
    if (disruption.getAffectedArea() != null && isMajorHub(disruption.getAffectedArea())) {
      severityScore += 20;
    }
    if (disruption.getAffectedStops() != null
        && disruption.getAffectedStops().stream().anyMatch(this::isMajorHub)) {
      severityScore += 10;
    }

    // Factor 4: Disruption type (0-10 points)
    if ("CANCELLATION".equalsIgnoreCase(disruption.getDisruptionType())) {
      severityScore += 10;
    }

    // Calculate final severity level
    String calculatedSeverity;
    if (severityScore >= 70) {
      calculatedSeverity = "CRITICAL";
    } else if (severityScore >= 50) {
      calculatedSeverity = "HIGH";
    } else if (severityScore >= 30) {
      calculatedSeverity = "MEDIUM";
    } else {
      calculatedSeverity = "LOW";
    }

    log.info("Calculated severity: {} (score: {})", calculatedSeverity, severityScore);
    return calculatedSeverity;
  }

  /**
   * Determine if immediate action is required
   *
   * @param disruption The disruption to evaluate
   * @return true if immediate response is needed
   */
  public boolean requiresImmediateAction(Disruption disruption) {
    log.debug("Checking if disruption {} requires immediate action", disruption.getId());

    // Immediate action if CRITICAL severity
    if ("CRITICAL".equalsIgnoreCase(disruption.getSeverity())) {
      log.info("✓ Immediate action required: CRITICAL severity");
      return true;
    }

    // Immediate action if service cancellation
    if ("CANCELLATION".equalsIgnoreCase(disruption.getDisruptionType())) {
      log.info("✓ Immediate action required: Service cancellation");
      return true;
    }

    // Immediate action if major hub affected
    if (disruption.getAffectedArea() != null && isMajorHub(disruption.getAffectedArea())) {
      log.info("✓ Immediate action required: Major hub affected");
      return true;
    }

    // Immediate action if severe delay (>30 minutes)
    Integer delayMinutes = disruption.getDelayMinutes();
    if (delayMinutes != null && delayMinutes >= CRITICAL_DELAY_THRESHOLD) {
      log.info("✓ Immediate action required: Severe delay ({} minutes)", delayMinutes);
      return true;
    }

    return false;
  }

  /**
   * Estimate the impact scope of a disruption
   *
   * @param disruption The disruption to analyze
   * @return Estimated number of affected travelers
   */
  public Integer estimateAffectedTravelers(Disruption disruption) {
    log.debug("Estimating affected travelers for disruption ID: {}", disruption.getId());

    // Base estimate per route (mock data - would come from real statistics)
    int basePassengersPerRoute = 200;

    // Calculate based on affected routes
    int affectedRouteCount = 0;
    if (disruption.getAffectedRoutes() != null) {
      affectedRouteCount = disruption.getAffectedRoutes().size();
    }

    int estimate = basePassengersPerRoute * Math.max(1, affectedRouteCount);

    // Multiply by severity factor
    String severity = disruption.getSeverity();
    if ("CRITICAL".equalsIgnoreCase(severity)) {
      estimate = (int) (estimate * 2.0); // Double for critical
    } else if ("HIGH".equalsIgnoreCase(severity)) {
      estimate = (int) (estimate * 1.5); // 50% increase for high
    }

    // Increase if major hub affected
    if (disruption.getAffectedArea() != null && isMajorHub(disruption.getAffectedArea())) {
      estimate = (int) (estimate * 1.5);
    }

    log.info("Estimated {} affected travelers", estimate);
    return estimate;
  }

  /** Helper method to check if a location is a major transport hub */
  private boolean isMajorHub(String location) {
    if (location == null) {
      return false;
    }
    return MAJOR_TRANSPORT_HUBS.stream()
        .anyMatch(
            hub ->
                location
                    .toLowerCase(java.util.Locale.ROOT)
                    .contains(hub.toLowerCase(java.util.Locale.ROOT)));
  }
}
