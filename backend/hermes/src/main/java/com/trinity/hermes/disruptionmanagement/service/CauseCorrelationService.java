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
 * Correlates the probable causes of a detected disruption using temporal co-occurrence — no spatial
 * math required.
 *
 * <ul>
 *   <li>Large event active today → EVENT (HIGH)
 *   <li>High car congestion currently active → CONGESTION (MEDIUM)
 *   <li>Another transport mode also disrupted simultaneously → CROSS_MODE (LOW)
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

    return causes;
  }

  // ── Large event today ──────────────────────────────────────────────

  private void checkEventCause(Disruption disruption, List<DisruptionCause> causes) {
    var events =
        eventsRepository.findUpcomingEventsAtLargeVenues(
            LARGE_EVENT_THRESHOLD, PageRequest.of(0, 10));
    events.stream()
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
                              + ")")
                      .confidence("HIGH")
                      .build());
            });
  }

  // ── High car congestion ────────────────────────────────────────────

  private void checkCongestionCause(Disruption disruption, List<DisruptionCause> causes) {
    List<Object[]> traffic = highTrafficPointsRepository.findAggregatedTrafficWithLocation();
    boolean highCongestion =
        traffic.stream()
            .filter(r -> r.length >= 3)
            .anyMatch(r -> ((Number) r[2]).longValue() >= HIGH_TRAFFIC_THRESHOLD);

    if (highCongestion) {
      causes.add(
          DisruptionCause.builder()
              .disruption(disruption)
              .causeType("CONGESTION")
              .causeDescription("High traffic congestion detected on Dublin road network")
              .confidence("MEDIUM")
              .build());
    }
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
