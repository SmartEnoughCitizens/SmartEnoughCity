package com.trinity.hermes.indicators.car.service;

import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.entity.TrafficRecommendation;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficRecommendationService {

  private static final int MAX_RECOMMENDATIONS = 4;
  private static final DecisionTreeNode CRITICAL_NODE =
      new DecisionTreeNode(0.85, "critical", 0.68, 0.27, 4.0, 8.0, 0.97, null);
  private static final DecisionTreeNode HIGH_NODE =
      new DecisionTreeNode(0.65, "high", 0.68, 0.27, 4.0, 8.0, 0.97, CRITICAL_NODE);
  private static final DecisionTreeNode ROOT_NODE =
      new DecisionTreeNode(
          Double.NEGATIVE_INFINITY, "elevated", 0.68, 0.27, 4.0, 8.0, 0.97, HIGH_NODE);

  private final HighTrafficPointsService highTrafficPointsService;

  @Transactional(readOnly = true)
  public Optional<TrafficRecommendation> getRecommendationById(String recommendationId) {
    return getTrafficRecommendations().stream()
        .filter(r -> recommendationId.equals(r.getRecommendationId()))
        .findFirst();
  }

  @Transactional(readOnly = true)
  public List<TrafficRecommendation> getTrafficRecommendations() {
    List<HighTrafficPointsDTO> trafficPoints = highTrafficPointsService.getHighTrafficPoints();
    if (trafficPoints.isEmpty()) {
      log.info("No traffic points available for recommendation generation");
      return List.of();
    }

    double maxVolume =
        trafficPoints.stream()
            .map(HighTrafficPointsDTO::getAvgVolume)
            .filter(volume -> volume != null)
            .max(Double::compareTo)
            .orElse(1.0);

    return trafficPoints.stream()
        .filter(
            point ->
                point.getLat() != null && point.getLon() != null && point.getAvgVolume() != null)
        .sorted(Comparator.comparing(HighTrafficPointsDTO::getAvgVolume).reversed())
        .limit(MAX_RECOMMENDATIONS)
        .map(point -> buildRecommendation(point, maxVolume))
        .toList();
  }

  private TrafficRecommendation buildRecommendation(HighTrafficPointsDTO point, double maxVolume) {
    double volume = point.getAvgVolume() == null ? 0.0 : point.getAvgVolume();
    double ratio = maxVolume <= 0 ? 0.0 : volume / maxVolume;
    DecisionTreeOutcome outcome = evaluateDecisionTree(ratio);

    return TrafficRecommendation.builder()
        .recommendationId(
            String.format(
                "traffic-%s-%s-%s", point.getSiteId(), point.getDayType(), point.getTimeSlot()))
        .siteId(point.getSiteId())
        .siteLat(point.getLat())
        .siteLon(point.getLon())
        .title(String.format("Diversion plan for Site %s", point.getSiteId()))
        .summary(
            String.format(
                "%s congestion detected during %s. Redirect through lower-pressure parallel links to reduce queue build-up.",
                capitalize(outcome.congestionLevel()), humanize(point.getTimeSlot())))
        .dayType(point.getDayType())
        .timeSlot(point.getTimeSlot())
        .averageVolume(volume)
        .congestionLevel(outcome.congestionLevel())
        .confidenceScore(outcome.confidenceScore())
        .recommendedAction(buildRecommendedAction(point, outcome.congestionLevel()))
        .generatedAt(OffsetDateTime.now(ZoneOffset.UTC).toString())
        .alternativeRoutes(buildAlternativeRoutes(point, outcome.baseTimeSavings()))
        .build();
  }

  private List<TrafficRecommendation.AlternativeRoute> buildAlternativeRoutes(
      HighTrafficPointsDTO point, int baseTimeSavings) {
    return IntStream.range(0, 2)
        .mapToObj(index -> buildAlternativeRoute(point, baseTimeSavings, index))
        .toList();
  }

  private TrafficRecommendation.AlternativeRoute buildAlternativeRoute(
      HighTrafficPointsDTO point, int baseTimeSavings, int routeIndex) {
    int timeSavings = Math.max(3, baseTimeSavings - routeIndex);
    int travelTime = Math.max(9, 22 - timeSavings + routeIndex * 2);
    double distanceKm = 2.6 + routeIndex * 0.9;
    String color = routeIndex == 0 ? "#0f766e" : "#ea580c";
    String label = routeIndex == 0 ? "Primary diversion route" : "Secondary diversion route";

    return TrafficRecommendation.AlternativeRoute.builder()
        .routeId(String.format("alt-%s-%d", point.getSiteId(), routeIndex + 1))
        .label(label)
        .summary(
            routeIndex == 0
                ? "Prioritise this corridor for the strongest expected queue reduction."
                : "Use as overflow when primary diversion approaches saturation.")
        .color(color)
        .estimatedTimeSavingsMinutes(timeSavings)
        .estimatedTravelTimeMinutes(travelTime)
        .distanceKm(distanceKm)
        .path(buildRoutePath(point, routeIndex))
        .build();
  }

  private List<TrafficRecommendation.RouteWaypoint> buildRoutePath(
      HighTrafficPointsDTO point, int routeIndex) {
    double lat = point.getLat();
    double lon = point.getLon();
    double latShift = routeIndex == 0 ? 0.004 : -0.0035;
    double lonShift = routeIndex == 0 ? -0.011 : 0.0105;

    return List.of(
        waypoint(lat - latShift, lon + lonShift * 0.15),
        waypoint(lat - latShift * 0.5, lon + lonShift * 0.55),
        waypoint(lat + latShift * 0.35, lon + lonShift * 0.85),
        waypoint(lat + latShift, lon + lonShift));
  }

  private TrafficRecommendation.RouteWaypoint waypoint(double lat, double lon) {
    return TrafficRecommendation.RouteWaypoint.builder().lat(lat).lon(lon).build();
  }

  private DecisionTreeOutcome evaluateDecisionTree(double ratio) {
    DecisionTreeNode matchedNode = ROOT_NODE.evaluate(ratio);
    double rawConfidenceScore =
        Math.min(
            matchedNode.maxConfidenceScore(),
            matchedNode.confidenceBase() + ratio * matchedNode.confidenceScale());
    int baseTimeSavings =
        Math.max(
            (int) Math.round(matchedNode.minTimeSavings()),
            (int)
                Math.round(matchedNode.timeSavingsBase() + ratio * matchedNode.timeSavingsScale()));

    return new DecisionTreeOutcome(
        matchedNode.congestionLevel(),
        Math.round(rawConfidenceScore * 100.0) / 100.0,
        baseTimeSavings);
  }

  private String buildRecommendedAction(HighTrafficPointsDTO point, String congestionLevel) {
    return String.format(
        "Activate %s diversion signage near Site %s for the %s %s window.",
        congestionLevel, point.getSiteId(), point.getDayType(), humanize(point.getTimeSlot()));
  }

  private String capitalize(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
  }

  private String humanize(String value) {
    if (value == null) {
      return "";
    }
    return value.replace('_', ' ');
  }

  private record DecisionTreeOutcome(
      String congestionLevel, double confidenceScore, int baseTimeSavings) {}

  private record DecisionTreeNode(
      double threshold,
      String congestionLevel,
      double confidenceBase,
      double confidenceScale,
      double timeSavingsBase,
      double timeSavingsScale,
      double maxConfidenceScore,
      DecisionTreeNode next) {

    private DecisionTreeNode evaluate(double ratio) {
      if (next != null && ratio >= next.threshold()) {
        return next.evaluate(ratio);
      }
      return this;
    }

    private double minTimeSavings() {
      return timeSavingsBase;
    }
  }
}
