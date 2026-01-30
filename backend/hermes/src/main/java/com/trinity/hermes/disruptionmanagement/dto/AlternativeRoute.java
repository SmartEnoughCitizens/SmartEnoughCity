package com.trinity.hermes.disruptionmanagement.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents an alternative route option for a disruption */
@Data
@NoArgsConstructor
public class AlternativeRoute {

  private String routeId;
  private String routeName;
  private List<String> transportModes; // Combined modes like [BUS, METRO]
  private List<String> stops; // Sequence of stops

  // Route Metrics
  private Integer estimatedDurationMinutes;
  private Integer additionalTimeMinutes; // Compared to original route
  private Double distanceKm;
  private Double estimatedCost;

  // Route Quality
  private Integer comfortScore; // 1-10
  private Integer reliabilityScore; // 1-10
  private Integer crowdingLevel; // 1-10

  // Recommendations
  private Boolean recommended; // Is this a recommended option?
  private Integer priority; // Lower number = higher priority
  private String notes;

  // Additional information

  public AlternativeRoute(
      String routeId,
      String routeName,
      List<String> transportModes,
      List<String> stops,
      Integer estimatedDurationMinutes,
      Integer additionalTimeMinutes,
      Double distanceKm,
      Double estimatedCost,
      Integer comfortScore,
      Integer reliabilityScore,
      Integer crowdingLevel,
      Boolean recommended,
      Integer priority,
      String notes) {

    this.routeId = routeId;
    this.routeName = routeName;
    this.transportModes = copyList(transportModes);
    this.stops = copyList(stops);
    this.estimatedDurationMinutes = estimatedDurationMinutes;
    this.additionalTimeMinutes = additionalTimeMinutes;
    this.distanceKm = distanceKm;
    this.estimatedCost = estimatedCost;
    this.comfortScore = comfortScore;
    this.reliabilityScore = reliabilityScore;
    this.crowdingLevel = crowdingLevel;
    this.recommended = recommended;
    this.priority = priority;
    this.notes = notes;
  }

  public List<String> getTransportModes() {
    return transportModes == null ? null : List.copyOf(transportModes);
  }

  public List<String> getStops() {
    return stops == null ? null : List.copyOf(stops);
  }

  public void setTransportModes(List<String> transportModes) {
    this.transportModes = copyList(transportModes);
  }

  public void setStops(List<String> stops) {
    this.stops = copyList(stops);
  }

  private static <T> List<T> copyList(List<T> in) {
    return in == null ? null : List.copyOf(in); // unmodifiable copy (Java 10+)
  }
}
