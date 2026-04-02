package com.trinity.hermes.indicators.car.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficRecommendation {

  private String recommendationId;
  private Integer siteId;
  private Double siteLat;
  private Double siteLon;
  private String title;
  private String summary;
  private String dayType;
  private String timeSlot;
  private Double averageVolume;
  private String congestionLevel;
  private Double confidenceScore;
  private String recommendedAction;
  private String generatedAt;
  private List<AlternativeRoute> alternativeRoutes;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AlternativeRoute {
    private String routeId;
    private String label;
    private String summary;
    private String color;
    private Integer estimatedTimeSavingsMinutes;
    private Integer estimatedTravelTimeMinutes;
    private Double distanceKm;
    private List<RouteWaypoint> path;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RouteWaypoint {
    private Double lat;
    private Double lon;
  }
}
