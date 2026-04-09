package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.dto.DisruptionDetectionRequest;
import com.trinity.hermes.disruptionmanagement.facade.DisruptionFacade;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionRepository;
import com.trinity.hermes.indicators.events.entity.Events;
import com.trinity.hermes.indicators.events.repository.EventsRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Proactive service-pressure alerts for scheduled events.
 *
 * <p>Unlike reactive disruptions (vehicles already delayed), service pressure is about events we
 * know about in advance. For each large event starting within the next {@value #LOOKAHEAD_HOURS}
 * hours, this service:
 *
 * <ul>
 *   <li>Finds bus routes with stops within {@value #VENUE_RADIUS_M} metres of the venue.
 *   <li>Finds Luas lines with stops within the same radius.
 *   <li>Creates a disruption record of type {@code EVENT} describing the expected demand surge,
 *       with an operator-facing recommendation to increase service on the affected routes.
 *   <li>Trams cannot be rerouted — they receive a warning notification only.
 * </ul>
 *
 * <p>Runs every 30 minutes so operators have lead time before the event starts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicePressureService {

  private static final ZoneId DUBLIN = ZoneId.of("Europe/Dublin");
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

  // How far ahead to look for upcoming events
  private static final int LOOKAHEAD_HOURS = 4;

  // Scan radius around the venue for transport stops
  private static final int VENUE_RADIUS_M = 500;

  // Minimum venue capacity to trigger an alert
  private static final int MIN_VENUE_CAPACITY = 1000;

  // Dedup: skip if an EVENT record for the same venue was already created recently
  private static final int DEDUP_WINDOW_MINUTES = 60;

  private final EventsRepository eventsRepository;
  private final DisruptionRepository disruptionRepository;
  private final DisruptionFacade disruptionFacade;
  private final DisruptionDetectionService detectionService;

  @Scheduled(fixedRate = 1_800_000, initialDelay = 60_000)
  public void detectUpcomingEventPressure() {
    log.info("=== SERVICE PRESSURE SCAN STARTED ===");
    int flagged = 0;

    try {
      LocalDateTime now = LocalDateTime.now(DUBLIN);
      LocalDateTime cutoff = now.plusHours(LOOKAHEAD_HOURS);

      List<Events> upcoming =
          eventsRepository.findUpcomingEventsAtLargeVenues(
              MIN_VENUE_CAPACITY, PageRequest.of(0, 30));

      for (Events ev : upcoming) {
        if (ev.getStartTime() == null) continue;
        if (ev.getStartTime().isAfter(cutoff)) continue; // too far ahead
        if (ev.getStartTime().isBefore(now)) continue; // already started
        if (ev.getLatitude() == null || ev.getLongitude() == null) continue;

        String area = ev.getVenueName() != null ? ev.getVenueName() : "Dublin Venue";
        String sourceRef = "event-" + (ev.getSourceId() != null ? ev.getSourceId() : ev.getId());

        // Dedup: skip if already flagged and still active
        if (disruptionRepository.existsByDisruptionTypeAndAffectedAreaAndStatus(
            "EVENT", area, "ACTIVE")) continue;
        LocalDateTime dedupCutoff = now.minusMinutes(DEDUP_WINDOW_MINUTES);
        if (!disruptionRepository
            .findByDisruptionTypeAndAffectedAreaAndDetectedAtAfter("EVENT", area, dedupCutoff)
            .isEmpty()) continue;

        // Find affected transport within venue radius
        List<String> busRoutes =
            detectionService.findBusRoutesNear(ev.getLatitude(), ev.getLongitude(), VENUE_RADIUS_M);
        List<String> tramLines =
            detectionService.findTramLinesNear(ev.getLatitude(), ev.getLongitude(), VENUE_RADIUS_M);

        int capacity = ev.getVenue() != null ? ev.getVenue().getCapacity() : 0;
        String severity = scoreSeverity(capacity);

        List<String> modes = buildModes(busRoutes, tramLines);
        String description =
            buildDescription(ev.getEventName(), area, ev.getStartTime(), busRoutes, tramLines);

        DisruptionDetectionRequest req =
            buildRequest(
                area,
                severity,
                description,
                ev.getLatitude(),
                ev.getLongitude(),
                modes,
                busRoutes,
                sourceRef,
                ev.getStartTime());

        try {
          disruptionFacade.handleDisruptionDetection(req);
          log.info(
              "Service pressure flagged: event='{}' venue='{}' capacity={} routes={}",
              ev.getEventName(),
              area,
              capacity,
              busRoutes);
          flagged++;
        } catch (Exception e) {
          log.error("Failed to flag service pressure for event '{}': {}", area, e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Service pressure scan failed: {}", e.getMessage());
    }

    log.info("=== SERVICE PRESSURE SCAN COMPLETE: {} event(s) flagged ===", flagged);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private String scoreSeverity(int capacity) {
    if (capacity >= 15000) return "CRITICAL"; // stadium-scale: Aviva, 3Arena, Croke Park
    if (capacity >= 5000) return "HIGH"; // large: RDS, Marlay Park
    return "MEDIUM"; // 1000–4999: Bord Gáis, 3Olympia, Ambassador, Gaiety, Vicar Street
  }

  private List<String> buildModes(List<String> busRoutes, List<String> tramLines) {
    List<String> modes = new ArrayList<>();
    if (!busRoutes.isEmpty()) modes.add("BUS");
    if (!tramLines.isEmpty()) modes.add("TRAM");
    modes.add("TRAIN"); // large events always affect rail demand
    return modes;
  }

  private String buildDescription(
      String eventName,
      String venue,
      LocalDateTime startTime,
      List<String> busRoutes,
      List<String> tramLines) {

    StringBuilder sb = new StringBuilder("Upcoming event");
    if (eventName != null) sb.append(" '").append(eventName).append("'");
    sb.append(" at ").append(venue);
    if (startTime != null) sb.append(" starting at ").append(startTime.format(TIME_FMT));
    sb.append(". Recommend increased service");

    if (!busRoutes.isEmpty()) {
      sb.append(" on bus routes ").append(String.join(", ", busRoutes));
    }
    if (!tramLines.isEmpty()) {
      sb.append(!busRoutes.isEmpty() ? " and on Luas " : " on Luas ")
          .append(String.join("/", tramLines))
          .append(" (warning only — trams cannot be rerouted)");
    }
    sb.append(".");
    return sb.toString();
  }

  private DisruptionDetectionRequest buildRequest(
      String area,
      String severity,
      String description,
      Double lat,
      Double lon,
      List<String> modes,
      List<String> routes,
      String sourceRef,
      LocalDateTime startTime) {

    DisruptionDetectionRequest req = new DisruptionDetectionRequest();
    req.setDisruptionType("EVENT");
    req.setSeverity(severity);
    req.setAffectedArea(area);
    req.setDescription(description);
    req.setLatitude(lat);
    req.setLongitude(lon);
    req.setDataSource("SCHEDULED_DETECTION");
    req.setSourceReferenceId(sourceRef);
    req.setEstimatedStartTime(startTime);
    req.setEstimatedEndTime(
        startTime != null ? startTime.plusHours(3) : LocalDateTime.now(DUBLIN).plusHours(3));
    req.setAffectedTransportModes(new ArrayList<>(modes));
    req.setAffectedRoutes(new ArrayList<>(routes));
    req.setAffectedStops(List.of());
    req.setDelayMinutes(null);
    return req;
  }
}
