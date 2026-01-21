package com.trinity.hermes.disruptionmanagement.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving disruption detection data from the Python data handler service. This is the
 * payload sent when the Python service detects a disruption.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisruptionDetectionRequest {

  // Basic Information
  private String disruptionType; // DELAY, CANCELLATION, CONGESTION, CONSTRUCTION, EVENT, ACCIDENT
  private String severity; // LOW, MEDIUM, HIGH, CRITICAL
  private String description;

  // Location Data
  private Double latitude;
  private Double longitude;
  private String affectedArea;

  // Transport Data
  private List<String> affectedTransportModes; // BUS, TRAM, TRAIN, METRO
  private List<String> affectedRoutes;
  private List<String> affectedStops;

  // Timing Information
  private LocalDateTime detectedAt;
  private LocalDateTime estimatedStartTime;
  private LocalDateTime estimatedEndTime;
  private Integer delayMinutes;

  // Source Information
  private String dataSource; // Which API or service detected this
  private String sourceReferenceId; // External reference ID

  // Additional Context
  private String eventName; // If disruption is due to an event
  private String constructionProject; // If disruption is due to construction
  private String trafficCongestionLevel; // If traffic congestion
  private String additionalNotes;
}
