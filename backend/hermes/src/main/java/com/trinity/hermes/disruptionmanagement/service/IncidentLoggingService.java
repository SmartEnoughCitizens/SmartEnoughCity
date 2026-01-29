package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for logging all disruption-related incidents and actions. Maintains audit
 * trail for optimization and system improvement.
 *
 * <p>NOTE: This implementation uses in-memory storage for the thin slice. In production, logs would
 * be persisted to a database or log aggregation system.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentLoggingService {

  // In-memory storage for incident logs (Map: DisruptionId -> List of log
  // entries)
  private final Map<Long, List<String>> incidentLogs = new ConcurrentHashMap<>();

  private static final DateTimeFormatter LOG_TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * Log a newly detected disruption
   *
   * @param disruption The detected disruption
   */
  public void logDisruptionDetected(Disruption disruption) {
    log.info(
        "Logging disruption detection - ID: {}, Type: {}, Severity: {}",
        disruption.getId(),
        disruption.getDisruptionType(),
        disruption.getSeverity());

    String logEntry =
        String.format(
            "[%s] [DETECTED] %s disruption in %s - Severity: %s | Delay: %d min | Routes: %s",
            getCurrentTimestamp(),
            disruption.getDisruptionType(),
            disruption.getAffectedArea(),
            disruption.getSeverity(),
            disruption.getDelayMinutes() != null ? disruption.getDelayMinutes() : 0,
            disruption.getAffectedRoutes() != null
                ? String.join(", ", disruption.getAffectedRoutes())
                : "N/A");

    addLogEntry(disruption.getId(), logEntry);
    log.info("✓ Detection logged for disruption {}", disruption.getId());
  }

  /**
   * Log the calculation of alternative routes
   *
   * @param disruptionId ID of the disruption
   * @param numberOfRoutes Number of alternative routes calculated
   * @param calculationTimeMs Time taken to calculate routes
   */
  public void logRouteCalculation(
      Long disruptionId, Integer numberOfRoutes, Long calculationTimeMs) {
    log.info(
        "Logging route calculation - Disruption ID: {}, Routes: {}, Time: {}ms",
        disruptionId,
        numberOfRoutes,
        calculationTimeMs);

    String logEntry =
        String.format(
            "[%s] [ROUTING] Calculated %d alternative routes in %dms",
            getCurrentTimestamp(), numberOfRoutes, calculationTimeMs);

    addLogEntry(disruptionId, logEntry);
    log.debug("✓ Route calculation logged");
  }

  /**
   * Log the compilation and rating of solutions
   *
   * @param disruptionId ID of the disruption
   * @param solutionDetails Details about the compiled solution
   */
  public void logSolutionCompilation(Long disruptionId, String solutionDetails) {
    log.info("Logging solution compilation - Disruption ID: {}", disruptionId);

    String logEntry =
        String.format(
            "[%s] [COMPILED] Solution compiled - %s", getCurrentTimestamp(), solutionDetails);

    addLogEntry(disruptionId, logEntry);
    log.debug("✓ Solution compilation logged");
  }

  /**
   * Log notification sent to users
   *
   * @param disruptionId ID of the disruption
   * @param numberOfUsers Number of users notified
   * @param notificationChannels Channels used for notification
   */
  public void logNotificationSent(
      Long disruptionId, Integer numberOfUsers, List<String> notificationChannels) {
    log.info(
        "Logging notification sent - Disruption ID: {}, Users: {}, Channels: {}",
        disruptionId,
        numberOfUsers,
        notificationChannels);

    String channels =
        notificationChannels != null ? String.join(", ", notificationChannels) : "N/A";
    String logEntry =
        String.format(
            "[%s] [NOTIFIED] Notifications sent to %d users via channels: %s",
            getCurrentTimestamp(), numberOfUsers != null ? numberOfUsers : 0, channels);

    addLogEntry(disruptionId, logEntry);
    log.debug("✓ Notification logged");
  }

  /**
   * Overloaded method to log notification success/failure
   *
   * @param disruptionId ID of the disruption
   * @param success Whether notification was successful
   */
  public void logNotificationSent(Long disruptionId, boolean success) {
    String logEntry =
        String.format(
            "[%s] [NOTIFIED] Notification status: %s",
            getCurrentTimestamp(), success ? "SUCCESS" : "FAILED");

    addLogEntry(disruptionId, logEntry);
    log.info("✓ Notification status logged: {}", success ? "SUCCESS" : "FAILED");
  }

  /**
   * Log the resolution of a disruption
   *
   * @param disruptionId ID of the disruption
   * @param resolutionNotes Notes about how it was resolved
   */
  public void logDisruptionResolved(Long disruptionId, String resolutionNotes) {
    log.info("Logging disruption resolution - ID: {}", disruptionId);

    String logEntry =
        String.format(
            "[%s] [RESOLVED] Disruption resolved - %s", getCurrentTimestamp(), resolutionNotes);

    addLogEntry(disruptionId, logEntry);
    log.info("✓ Resolution logged for disruption {}", disruptionId);
  }

  /**
   * Log system performance metrics for a disruption response
   *
   * @param disruptionId ID of the disruption
   * @param detectionLatencyMs Time from occurrence to detection
   * @param responseLatencyMs Time from detection to notification
   */
  public void logPerformanceMetrics(
      Long disruptionId, Long detectionLatencyMs, Long responseLatencyMs) {
    log.info(
        "Logging performance metrics - Disruption ID: {}, Detection: {}ms, Response: {}ms",
        disruptionId,
        detectionLatencyMs,
        responseLatencyMs);

    String logEntry =
        String.format(
            "[%s] [METRICS] Performance - Detection latency: %dms, Response latency: %dms, Total: %dms",
            getCurrentTimestamp(),
            detectionLatencyMs,
            responseLatencyMs,
            detectionLatencyMs + responseLatencyMs);

    addLogEntry(disruptionId, logEntry);
    log.debug("✓ Performance metrics logged");
  }

  /**
   * Retrieve incident logs for a specific disruption
   *
   * @param disruptionId ID of the disruption
   * @return List of log entries in chronological order
   */
  public List<String> getIncidentLogs(Long disruptionId) {
    log.debug("Retrieving incident logs for disruption ID: {}", disruptionId);

    List<String> logs = incidentLogs.get(disruptionId);
    if (logs == null || logs.isEmpty()) {
      log.debug("No logs found for disruption ID: {}", disruptionId);
      return new ArrayList<>();
    }

    log.debug("Retrieved {} log entries for disruption {}", logs.size(), disruptionId);
    return new ArrayList<>(logs); // Return copy to prevent external modification
  }

  /**
   * Generate analytics report for disruption patterns
   *
   * @param startDate Start date for analysis
   * @param endDate End date for analysis
   * @return Analytics report
   */
  public String generateAnalyticsReport(LocalDateTime startDate, LocalDateTime endDate) {
    log.info("Generating analytics report from {} to {}", startDate, endDate);

    // For thin slice, return basic summary
    int totalDisruptions = incidentLogs.size();
    int totalLogEntries = incidentLogs.values().stream().mapToInt(List::size).sum();

      String report =
              String.format(
                      "===== DISRUPTION ANALYTICS REPORT =====%n" +
                              "Period: %s to %s%n" +
                              "Total Disruptions Tracked: %d%n" +
                              "Total Log Entries: %d%n" +
                              "Average Entries per Disruption: %.2f%n%n" +
                              "NOTE: This is a basic summary for the thin slice.%n" +
                              "Full analytics would include:%n" +
                              "- Disruption patterns and trends%n" +
                              "- Response time analysis%n" +
                              "- Effectiveness metrics%n" +
                              "- Peak disruption times%n" +
                              "========================================%n",
            startDate != null ? startDate.format(LOG_TIMESTAMP_FORMAT) : "N/A",
            endDate != null ? endDate.format(LOG_TIMESTAMP_FORMAT) : "N/A",
            totalDisruptions,
            totalLogEntries,
            totalDisruptions > 0 ? (double) totalLogEntries / totalDisruptions : 0.0);

    log.info("Analytics report generated");
    return report;
  }

  /**
   * Get total number of disruptions logged
   *
   * @return Count of disruptions
   */
  public int getDisruptionCount() {
    return incidentLogs.size();
  }

  /** Clear all logs (for testing purposes) */
  public void clearAllLogs() {
    log.warn("Clearing all incident logs");
    incidentLogs.clear();
  }

  // ===== PRIVATE HELPER METHODS =====

  private void addLogEntry(Long disruptionId, String logEntry) {
    incidentLogs.computeIfAbsent(disruptionId, k -> new ArrayList<>()).add(logEntry);

    // Also log to SLF4J for console/file logging
    log.info(logEntry);
  }

  private String getCurrentTimestamp() {
    return LocalDateTime.now(java.time.ZoneId.of("Europe/Dublin")).format(LOG_TIMESTAMP_FORMAT);
  }
}
