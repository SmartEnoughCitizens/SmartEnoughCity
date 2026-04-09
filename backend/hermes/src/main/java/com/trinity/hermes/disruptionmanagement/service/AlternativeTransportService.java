package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.dto.AlternativeTransportResult;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.entity.DisruptionAlternative;
import com.trinity.hermes.disruptionmanagement.repository.AlternativeTransportRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds nearby alternative transport for a disruption, excluding the disrupted mode itself.
 *
 * <p>For example, a bus disruption will suggest nearby rail stations, Luas stops, and DublinBikes
 * docks — not other bus stops, which would be equally affected. Events use all modes since there is
 * no single disrupted mode to exclude.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlternativeTransportService {

  private static final int DEFAULT_RADIUS_METRES = 500;

  private final AlternativeTransportRepository alternativeTransportRepository;

  /**
   * Returns nearby alternatives for the given disruption, excluding the disrupted transport mode.
   * Returns an empty list if the disruption has no coordinates.
   *
   * @param disruption the detected disruption
   * @return list of DisruptionAlternative entities (not yet persisted)
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public List<DisruptionAlternative> getAlternatives(Disruption disruption) {
    if (disruption.getLatitude() == null || disruption.getLongitude() == null) {
      log.debug(
          "Disruption {} has no coordinates — skipping alternative lookup", disruption.getId());
      return List.of();
    }

    try {
      Set<String> excluded = excludedTypesFor(disruption);
      List<AlternativeTransportResult> nearby =
          alternativeTransportRepository.findNearbyExcluding(
              disruption.getLatitude(), disruption.getLongitude(), DEFAULT_RADIUS_METRES, excluded);

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
   * Convenience overload — look up all alternatives by explicit coordinates (used by
   * TramDashboardService when it already has a stop's lat/lon).
   */
  public List<AlternativeTransportResult> findNearby(double lat, double lon) {
    return alternativeTransportRepository.findNearby(lat, lon, DEFAULT_RADIUS_METRES);
  }

  /**
   * Maps disruption transport modes to the query transport-type strings to exclude. BUS → "bus";
   * TRAIN → "rail"; TRAM → "tram". EVENT disruptions include all modes (no exclusion).
   */
  private Set<String> excludedTypesFor(Disruption disruption) {
    List<String> modes = disruption.getAffectedTransportModes();
    if (modes == null || modes.isEmpty()) return Set.of();
    // EVENT / SERVICE_PRESSURE: include all modes around the venue
    if ("EVENT".equals(disruption.getDisruptionType())) return Set.of();
    Set<String> excluded = new java.util.HashSet<>();
    for (String mode : modes) {
      switch (mode.toUpperCase(java.util.Locale.ROOT)) {
        case "BUS" -> excluded.add("bus");
        case "TRAIN" -> excluded.add("rail");
        case "TRAM" -> excluded.add("tram");
        default -> {} // CAR, CYCLE — no direct exclusion
      }
    }
    return excluded;
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
      case "bus" -> "Bus stop: " + r.stopName() + " (" + r.distanceM() + "m walk)";
      case "rail" -> "Irish Rail station: " + r.stopName() + " (" + r.distanceM() + "m walk)";
      case "tram" -> "Luas stop: " + r.stopName() + " (" + r.distanceM() + "m walk)";
      case "bike" ->
          "DublinBikes: "
              + r.stopName()
              + " — "
              + (r.availableBikes() != null ? r.availableBikes() : 0)
              + " bikes available ("
              + r.distanceM()
              + "m walk)";
      default -> r.stopName() + " (" + r.distanceM() + "m walk)";
    };
  }
}
