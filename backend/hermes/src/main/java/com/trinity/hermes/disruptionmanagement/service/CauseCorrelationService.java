package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.entity.DisruptionCause;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionRepository;
import com.trinity.hermes.indicators.car.repository.HighTrafficPointsRepository;
import com.trinity.hermes.indicators.events.repository.EventsRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Correlates the probable causes of a detected disruption using temporal co-occurrence and spatial
 * proximity.
 *
 * <ul>
 *   <li>Large event within 2km and within the time window → EVENT (HIGH)
 *   <li>High car congestion currently active → CONGESTION (MEDIUM)
 *   <li>Another transport mode also disrupted simultaneously → CROSS_MODE (LOW)
 *   <li>No cause found → UNKNOWN with route-aware fallback message
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CauseCorrelationService {

  private static final int LARGE_EVENT_THRESHOLD = 1000;
  private static final long HIGH_TRAFFIC_THRESHOLD = 1500L;
  private static final int CONCURRENT_WINDOW_MINUTES = 15;
  private static final ZoneId DUBLIN = ZoneId.of("Europe/Dublin");

  private static final double EVENT_CAUSE_RADIUS_KM = 2.0;
  private static final int EVENT_WINDOW_HOURS_AFTER = 4;
  private static final int EVENT_WINDOW_HOURS_BEFORE = 2;

  private final EventsRepository eventsRepository;
  private final HighTrafficPointsRepository highTrafficPointsRepository;
  private final DisruptionRepository disruptionRepository;

  /**
   * Returns a list of correlated causes for the given disruption. Does not persist — caller is
   * responsible for saving.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<DisruptionCause> correlateCauses(Disruption disruption) {
    List<DisruptionCause> causes = new ArrayList<>();

    try {
      checkEventCause(disruption, causes);
    } catch (Exception e) {
      log.warn("Event cause check failed: {}", e.getMessage());
    }

    try {
      checkCongestionCause(disruption, causes);
    } catch (Exception e) {
      log.warn("Congestion cause check failed: {}", e.getMessage());
    }

    try {
      checkCrossModeCause(disruption, causes);
    } catch (Exception e) {
      log.warn("Cross-mode cause check failed: {}", e.getMessage());
    }

    try {
      checkFallbackAndEnrich(disruption, causes);
    } catch (Exception e) {
      log.warn("Fallback cause check failed: {}", e.getMessage());
    }

    return causes;
  }

  // ── Large event within 2km and within time window ─────────────────

  private void checkEventCause(Disruption disruption, List<DisruptionCause> causes) {
    if (disruption.getLatitude() == null || disruption.getLongitude() == null) return;

    LocalDateTime now = LocalDateTime.now(DUBLIN);
    LocalDateTime windowStart = now.minusHours(EVENT_WINDOW_HOURS_BEFORE);
    LocalDateTime windowEnd = now.plusHours(EVENT_WINDOW_HOURS_AFTER);

    List<com.trinity.hermes.indicators.events.entity.Events> events =
        eventsRepository.findUpcomingEventsAtLargeVenues(
            LARGE_EVENT_THRESHOLD, PageRequest.of(0, 50));

    events.stream()
        .filter(e -> e.getLatitude() != null && e.getLongitude() != null)
        .filter(
            e ->
                haversineKm(
                        disruption.getLatitude(),
                        disruption.getLongitude(),
                        e.getLatitude(),
                        e.getLongitude())
                    <= EVENT_CAUSE_RADIUS_KM)
        .filter(
            e -> {
              LocalDateTime start = e.getStartTime();
              LocalDateTime end = e.getEndTime();
              if (start == null) return false;
              if (end != null) return end.isAfter(windowStart) && start.isBefore(windowEnd);
              return start.isAfter(windowStart) && start.isBefore(windowEnd);
            })
        .findFirst()
        .ifPresent(
            e -> {
              String venue = e.getVenueName() != null ? e.getVenueName() : "nearby venue";
              int capacity = e.getVenue() != null ? e.getVenue().getCapacity() : 0;
              causes.add(
                  DisruptionCause.builder()
                      .disruption(disruption)
                      .causeType("EVENT")
                      .causeDescription(
                          "Large event '"
                              + (e.getEventName() != null ? e.getEventName() : "event")
                              + "' at "
                              + venue
                              + " (venue capacity: "
                              + capacity
                              + ") within 2km")
                      .confidence("HIGH")
                      .build());
            });
  }

  private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
    final double R = 6371.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  // ── High car congestion ────────────────────────────────────────────

  private void checkCongestionCause(Disruption disruption, List<DisruptionCause> causes) {
    // MV returns [site_id, lat, lon, max_volume]; fallback returns same column layout
    List<Object[]> traffic;
    try {
      traffic = highTrafficPointsRepository.findPeakTrafficSitesFromMv();
    } catch (Exception e) {
      traffic = highTrafficPointsRepository.findPeakTrafficSitesWithLocation();
    }

    // Only flag congestion if a peak site is physically near this disruption
    boolean highCongestionNearby =
        disruption.getLatitude() != null
            && disruption.getLongitude() != null
            && traffic.stream()
                .filter(r -> r.length >= 4 && r[1] != null && r[2] != null && r[3] != null)
                .filter(r -> ((Number) r[3]).longValue() >= HIGH_TRAFFIC_THRESHOLD)
                .anyMatch(
                    r ->
                        haversineKm(
                                disruption.getLatitude(),
                                disruption.getLongitude(),
                                ((Number) r[1]).doubleValue(),
                                ((Number) r[2]).doubleValue())
                            <= EVENT_CAUSE_RADIUS_KM);

    if (highCongestionNearby) {
      causes.add(
          DisruptionCause.builder()
              .disruption(disruption)
              .causeType("CONGESTION")
              .causeDescription("High traffic congestion detected near disruption location")
              .confidence("MEDIUM")
              .build());
    }
  }

  // ── Fallback + enrichment when no cause found ─────────────────────

  private void checkFallbackAndEnrich(Disruption disruption, List<DisruptionCause> causes) {
    List<String> routes = disruption.getAffectedRoutes();
    int routeCount = routes != null ? routes.size() : 0;

    if (causes.isEmpty()) {
      String msg;
      if (routeCount == 1) {
        msg =
            "No external cause identified; may be a vehicle or crew issue specific to route "
                + routes.get(0)
                + ".";
      } else if (routeCount > 1) {
        msg =
            "Multiple routes affected at this stop simultaneously; possible stop-level"
                + " obstruction or infrastructure issue.";
      } else {
        msg = "No external cause identified; likely a service-specific technical issue.";
      }
      causes.add(
          DisruptionCause.builder()
              .disruption(disruption)
              .causeType("UNKNOWN")
              .causeDescription(msg)
              .confidence("LOW")
              .build());
    }

    causes.stream()
        .filter(c -> "CROSS_MODE".equals(c.getCauseType()))
        .findFirst()
        .ifPresent(
            c -> {
              List<String> modes = disruption.getAffectedTransportModes();
              if (modes != null && !modes.isEmpty()) {
                c.setCauseDescription(
                    "Simultaneous disruption across: "
                        + String.join(", ", modes)
                        + ". Possible network-wide pressure.");
              }
              long activeCount = disruptionRepository.countByStatus("ACTIVE");
              if (activeCount >= 100) {
                c.setConfidence("HIGH");
                c.setCauseDescription(
                    "City-wide disruption pattern (100+ active incidents) — possible large-scale"
                        + " event or infrastructure failure. "
                        + c.getCauseDescription());
              }
            });
  }

  // ── Another mode simultaneously disrupted ─────────────────────────

  private void checkCrossModeCause(Disruption disruption, List<DisruptionCause> causes) {
    LocalDateTime cutoff = LocalDateTime.now(DUBLIN).minusMinutes(CONCURRENT_WINDOW_MINUTES);
    List<Disruption> concurrent = disruptionRepository.findAllActiveOrderByDetectedAtDesc();

    boolean otherModeActive =
        concurrent.stream()
            .filter(d -> !d.getId().equals(disruption.getId()))
            .filter(d -> d.getDetectedAt() != null && d.getDetectedAt().isAfter(cutoff))
            .anyMatch(
                d ->
                    d.getAffectedTransportModes() != null
                        && disruption.getAffectedTransportModes() != null
                        && d.getAffectedTransportModes().stream()
                            .noneMatch(m -> disruption.getAffectedTransportModes().contains(m)));

    if (otherModeActive) {
      causes.add(
          DisruptionCause.builder()
              .disruption(disruption)
              .causeType("CROSS_MODE")
              .causeDescription("Simultaneous disruption detected across multiple transport modes")
              .confidence("LOW")
              .build());
    }
  }
}
