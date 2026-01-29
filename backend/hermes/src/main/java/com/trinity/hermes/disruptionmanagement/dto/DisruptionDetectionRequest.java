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

    public DisruptionDetectionRequest(
            String disruptionType,
            String severity,
            String description,
            Double latitude,
            Double longitude,
            String affectedArea,
            List<String> affectedTransportModes,
            List<String> affectedRoutes,
            List<String> affectedStops,
            LocalDateTime detectedAt,
            LocalDateTime estimatedStartTime,
            LocalDateTime estimatedEndTime,
            Integer delayMinutes,
            String dataSource,
            String sourceReferenceId,
            String eventName,
            String constructionProject,
            String trafficCongestionLevel,
            String additionalNotes) {

        this.disruptionType = disruptionType;
        this.severity = severity;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.affectedArea = affectedArea;

        this.affectedTransportModes = copyList(affectedTransportModes);
        this.affectedRoutes = copyList(affectedRoutes);
        this.affectedStops = copyList(affectedStops);

        this.detectedAt = detectedAt;
        this.estimatedStartTime = estimatedStartTime;
        this.estimatedEndTime = estimatedEndTime;
        this.delayMinutes = delayMinutes;

        this.dataSource = dataSource;
        this.sourceReferenceId = sourceReferenceId;

        this.eventName = eventName;
        this.constructionProject = constructionProject;
        this.trafficCongestionLevel = trafficCongestionLevel;
        this.additionalNotes = additionalNotes;
    }

    public void setAffectedTransportModes(List<String> affectedTransportModes) {
        this.affectedTransportModes = copyList(affectedTransportModes);
    }

    public void setAffectedRoutes(List<String> affectedRoutes) {
        this.affectedRoutes = copyList(affectedRoutes);
    }

    public void setAffectedStops(List<String> affectedStops) {
        this.affectedStops = copyList(affectedStops);
    }

    public List<String> getAffectedTransportModes() {
        return affectedTransportModes == null ? null : List.copyOf(affectedTransportModes);
    }

    public List<String> getAffectedRoutes() {
        return affectedRoutes == null ? null : List.copyOf(affectedRoutes);
    }

    public List<String> getAffectedStops() {
        return affectedStops == null ? null : List.copyOf(affectedStops);
    }

    private static <T> List<T> copyList(List<T> in) {
        return in == null ? null : List.copyOf(in); // Java 10+
    }
}
