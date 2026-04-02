package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.dto.AlternativeTransportResult;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.entity.DisruptionAlternative;
import com.trinity.hermes.disruptionmanagement.repository.AlternativeTransportRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds nearby alternative transport for any disruption type using the unified
 * AlternativeTransportRepository. Caller passes the disruption's lat/lon — no mode-specific special
 * casing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlternativeTransportService {

  private static final int DEFAULT_RADIUS_METRES = 500;

  private final AlternativeTransportRepository alternativeTransportRepository;

  /**
   * Returns nearby alternatives for the given disruption. Returns an empty list if the disruption
   * has no coordinates.
   *
   * @param disruption the detected disruption
   * @return list of DisruptionAlternative entities (not yet persisted)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<DisruptionAlternative> getAlternatives(Disruption disruption) {
    if (disruption.getLatitude() == null || disruption.getLongitude() == null) {
      log.debug(
          "Disruption {} has no coordinates — skipping alternative lookup", disruption.getId());
      return List.of();
    }

    try {
      List<AlternativeTransportResult> nearby =
          alternativeTransportRepository.findNearby(
              disruption.getLatitude(), disruption.getLongitude(), DEFAULT_RADIUS_METRES);

      return nearby.stream().map(r -> toEntity(r, disruption)).collect(Collectors.toList());
    } catch (Exception e) {
      log.warn(
          "Alternative transport lookup failed for disruption {}: {}",
          disruption.getId(),
          e.getMessage());
      return List.of();
    }
  }

  /**
   * Convenience overload — look up alternatives by explicit coordinates (used by
   * TramDashboardService when it already has a stop's lat/lon).
   */
  public List<AlternativeTransportResult> findNearby(double lat, double lon) {
    return alternativeTransportRepository.findNearby(lat, lon, DEFAULT_RADIUS_METRES);
  }

  private DisruptionAlternative toEntity(AlternativeTransportResult r, Disruption disruption) {
    String description = buildDescription(r);
    return DisruptionAlternative.builder()
        .disruption(disruption)
        .mode(r.transportType())
        .description(description)
        .stopName(r.stopName())
        .availabilityCount(r.availableBikes())
        .lat(r.lat())
        .lon(r.lon())
        .build();
  }

  private String buildDescription(AlternativeTransportResult r) {
    return switch (r.transportType()) {
      case "bus" -> "Bus stop: " + r.stopName() + " (" + r.distanceM() + "m away)";
      case "rail" -> "Irish Rail: " + r.stopName() + " (" + r.distanceM() + "m away)";
      case "bike" ->
          "DublinBikes: "
              + r.stopName()
              + " — "
              + r.availableBikes()
              + " bikes available ("
              + r.distanceM()
              + "m away)";
      default -> r.stopName() + " (" + r.distanceM() + "m away)";
    };
  }
}
