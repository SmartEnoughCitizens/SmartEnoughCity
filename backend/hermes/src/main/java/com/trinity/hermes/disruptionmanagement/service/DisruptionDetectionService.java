package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.dto.DisruptionDetectionRequest;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.facade.DisruptionFacade;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionRepository;
import com.trinity.hermes.indicators.bus.repository.BusRouteMetricsRepository;
import com.trinity.hermes.indicators.car.repository.HighTrafficPointsRepository;
import com.trinity.hermes.indicators.events.repository.EventsRepository;
import com.trinity.hermes.indicators.train.entity.TrainStationData;
import com.trinity.hermes.indicators.train.repository.TrainStationDataRepository;
import com.trinity.hermes.indicators.train.repository.TrainStationRepository;
import com.trinity.hermes.indicators.tram.entity.TramDisruptionExternal;
import com.trinity.hermes.indicators.tram.entity.TramStop;
import com.trinity.hermes.indicators.tram.repository.TramDisruptionExternalRepository;
import com.trinity.hermes.indicators.tram.repository.TramStopRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service that automatically detects live disruptions every 5 minutes by correlating data
 * from all transport modes, scoring severity, and triggering notifications for high-severity
 * events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisruptionDetectionService {

  private static final ZoneId DUBLIN = ZoneId.of("Europe/Dublin");

  // Thresholds for detection
  private static final double BUS_LATE_ARRIVAL_THRESHOLD_PCT = 40.0; // >40% late → disruption
  private static final double BUS_AVG_DELAY_THRESHOLD_SECONDS = 600.0; // >10 min avg delay
  private static final int BUS_MAX_DELAY_THRESHOLD_SECONDS = 1800; // >30 min max delay
  private static final long HIGH_TRAFFIC_VOLUME_THRESHOLD = 1500L; // vehicles per period
  private static final int LARGE_EVENT_ATTENDANCE_THRESHOLD = 5000; // estimated attendees
  private static final int DEDUP_WINDOW_MINUTES = 10; // ignore same disruption within 10 min
  private static final int TRAIN_LATE_THRESHOLD_MINUTES = 10; // lateMinutes > 10
  private static final int TRAIN_MIN_LATE_TRAINS = 3; // ≥3 late trains at same station

  private final BusRouteMetricsRepository busRouteMetricsRepository;
  private final HighTrafficPointsRepository highTrafficPointsRepository;
  private final EventsRepository eventsRepository;
  private final DisruptionRepository disruptionRepository;
  private final DisruptionFacade disruptionFacade;
  private final TrainStationDataRepository trainStationDataRepository;
  private final TrainStationRepository trainStationRepository;
  private final TramDisruptionExternalRepository tramDisruptionExternalRepository;
  private final TramStopRepository tramStopRepository;

  /**
   * Main scheduled detection task — runs every 5 minutes. Queries all transport data sources and
   * creates disruptions for any anomalies detected.
   */
  @Scheduled(fixedRate = 300_000, initialDelay = 15_000)
  public void detectDisruptions() {
    log.info("=== DISRUPTION AUTO-DETECTION CYCLE STARTED ===");
    int detected = 0;

    detected += detectBusDisruptions();
    detected += detectCarCongestionDisruptions();
    detected += detectEventDisruptions();
    detected += detectTrainDisruptions();
    detected += detectTramDisruptions();

    log.info("=== DISRUPTION AUTO-DETECTION CYCLE COMPLETE: {} new disruption(s) ===", detected);
  }

  // ---------------------------------------------------------------------------
  // BUS — detect routes with high delays or low reliability
  // ---------------------------------------------------------------------------

  private int detectBusDisruptions() {
    int count = 0;
    try {
      List<com.trinity.hermes.indicators.bus.entity.BusRouteMetrics> metrics =
          busRouteMetricsRepository.findCandidatesForDisruptionDetection();

      for (com.trinity.hermes.indicators.bus.entity.BusRouteMetrics m : metrics) {
        String area = buildBusArea(m);

        // Check for high late-arrival percentage
        if (m.getLateArrivalPct() != null
            && m.getLateArrivalPct() > BUS_LATE_ARRIVAL_THRESHOLD_PCT) {
          int delayMinutes = deriveDelayMinutes(m.getAvgDelaySeconds());
          String severity = scoreBusSeverity(m);
          if (processIfNew("DELAY", "BUS", area, severity, delayMinutes, m.getRouteId())) {
            count++;
          }
        }

        // Check for extreme single-route max delay
        if (m.getMaxDelaySeconds() != null
            && m.getMaxDelaySeconds() > BUS_MAX_DELAY_THRESHOLD_SECONDS) {
          int delayMinutes = m.getMaxDelaySeconds() / 60;
          if (processIfNew("DELAY", "BUS", area, "HIGH", delayMinutes, m.getRouteId())) {
            count++;
          }
        }
      }
    } catch (Exception e) {
      log.warn("Bus disruption detection failed: {}", e.getMessage());
    }
    return count;
  }

  private String buildBusArea(com.trinity.hermes.indicators.bus.entity.BusRouteMetrics m) {
    if (m.getRouteLongName() != null && !m.getRouteLongName().isBlank()) {
      return m.getRouteLongName();
    }
    if (m.getRouteShortName() != null) {
      return "Bus Route " + m.getRouteShortName();
    }
    return "Dublin Bus Network";
  }

  private int deriveDelayMinutes(Double avgDelaySeconds) {
    if (avgDelaySeconds == null) return 0;
    return (int) (avgDelaySeconds / 60.0);
  }

  private String scoreBusSeverity(com.trinity.hermes.indicators.bus.entity.BusRouteMetrics m) {
    double late = m.getLateArrivalPct() != null ? m.getLateArrivalPct() : 0;
    double avgDelay = m.getAvgDelaySeconds() != null ? m.getAvgDelaySeconds() : 0;
    if (late > 70 || avgDelay > BUS_AVG_DELAY_THRESHOLD_SECONDS * 2) return "CRITICAL";
    if (late > 55 || avgDelay > BUS_AVG_DELAY_THRESHOLD_SECONDS * 1.5) return "HIGH";
    if (late > BUS_LATE_ARRIVAL_THRESHOLD_PCT) return "MEDIUM";
    return "LOW";
  }

  // ---------------------------------------------------------------------------
  // CAR — detect high-congestion sites
  // ---------------------------------------------------------------------------

  private int detectCarCongestionDisruptions() {
    int count = 0;
    try {
      List<Object[]> rows = highTrafficPointsRepository.findAggregatedTrafficWithLocation();

      for (Object[] row : rows) {
        if (row.length < 3) continue;
        long volume = ((Number) row[2]).longValue();
        if (volume < HIGH_TRAFFIC_VOLUME_THRESHOLD) continue;

        String siteId = row[0] != null ? row[0].toString() : "unknown";
        String area = "Traffic Site " + siteId;
        Double lat = row.length > 3 && row[3] != null ? ((Number) row[3]).doubleValue() : null;
        Double lon = row.length > 4 && row[4] != null ? ((Number) row[4]).doubleValue() : null;
        String severity = volume > HIGH_TRAFFIC_VOLUME_THRESHOLD * 2 ? "HIGH" : "MEDIUM";

        if (processIfNewWithCoords("CONGESTION", "CAR", area, severity, 0, siteId, lat, lon)) {
          count++;
        }
      }
    } catch (Exception e) {
      log.warn("Car congestion disruption detection failed: {}", e.getMessage());
    }
    return count;
  }

  // ---------------------------------------------------------------------------
  // EVENTS — large events can disrupt nearby transport
  // ---------------------------------------------------------------------------

  private int detectEventDisruptions() {
    int count = 0;
    try {
      List<com.trinity.hermes.indicators.events.entity.Events> events =
          eventsRepository.findUpcomingEvents(
              org.springframework.data.domain.PageRequest.of(0, 20));

      for (com.trinity.hermes.indicators.events.entity.Events ev : events) {
        if (ev.getEstimatedAttendance() == null
            || ev.getEstimatedAttendance() < LARGE_EVENT_ATTENDANCE_THRESHOLD) {
          continue;
        }

        String area = ev.getVenueName() != null ? ev.getVenueName() : "Dublin";
        String severity =
            ev.getEstimatedAttendance() > 20000
                ? "HIGH"
                : ev.getEstimatedAttendance() > 10000 ? "MEDIUM" : "LOW";

        if (processIfNewWithCoords(
            "EVENT",
            "BUS,TRAM,TRAIN",
            area,
            severity,
            0,
            ev.getSourceId() != null ? ev.getSourceId() : String.valueOf(ev.getId()),
            ev.getLatitude(),
            ev.getLongitude())) {
          count++;
        }
      }
    } catch (Exception e) {
      log.warn("Event disruption detection failed: {}", e.getMessage());
    }
    return count;
  }

  // ---------------------------------------------------------------------------
  // TRAIN — detect stations with ≥3 late trains (lateMinutes > 10)
  // ---------------------------------------------------------------------------

  private int detectTrainDisruptions() {
    int count = 0;
    try {
      List<TrainStationData> latest = trainStationDataRepository.findLatestPerStationTrain();

      // Group by station, count how many trains are late there
      Map<String, List<TrainStationData>> byStation =
          latest.stream()
              .filter(
                  sd ->
                      sd.getLateMinutes() != null
                          && sd.getLateMinutes() > TRAIN_LATE_THRESHOLD_MINUTES)
              .collect(Collectors.groupingBy(TrainStationData::getStationCode));

      for (Map.Entry<String, List<TrainStationData>> entry : byStation.entrySet()) {
        if (entry.getValue().size() < TRAIN_MIN_LATE_TRAINS) continue;

        String stationCode = entry.getKey();
        int maxDelay =
            entry.getValue().stream()
                .mapToInt(TrainStationData::getLateMinutes)
                .max()
                .orElse(0);
        String severity = maxDelay > 30 ? "HIGH" : maxDelay > 20 ? "MEDIUM" : "LOW";

        // Look up lat/lon from stations table
        com.trinity.hermes.indicators.train.entity.TrainStation station =
            trainStationRepository.findByStationCode(stationCode);
        Double lat = station != null ? station.getLat() : null;
        Double lon = station != null ? station.getLon() : null;
        String area =
            station != null && station.getStationDesc() != null
                ? station.getStationDesc()
                : "Train Station " + stationCode;

        if (processIfNewWithCoords("DELAY", "TRAIN", area, severity, maxDelay, stationCode, lat, lon)) {
          count++;
        }
      }
    } catch (Exception e) {
      log.warn("Train disruption detection failed: {}", e.getMessage());
    }
    return count;
  }

  // ---------------------------------------------------------------------------
  // TRAM — bridge unresolved rows from external_data.tram_disruptions
  // ---------------------------------------------------------------------------

  private int detectTramDisruptions() {
    int count = 0;
    try {
      LocalDateTime since = LocalDateTime.now(DUBLIN).minusMinutes(DEDUP_WINDOW_MINUTES + 5);
      List<TramDisruptionExternal> active =
          tramDisruptionExternalRepository.findActiveDisruptionsSince(since);

      // Build a stop-id → TramStop lookup for lat/lon
      Map<String, TramStop> stopMap =
          tramStopRepository.findAll().stream()
              .collect(Collectors.toMap(TramStop::getStopId, s -> s, (a, b) -> a));

      for (TramDisruptionExternal ext : active) {
        TramStop stop = stopMap.get(ext.getStopId());
        Double lat = stop != null ? stop.getLat() : null;
        Double lon = stop != null ? stop.getLon() : null;
        String area = stop != null ? stop.getName() : "Tram Stop " + ext.getStopId();

        if (processIfNewWithCoords(
            "TRAM_DISRUPTION", "TRAM", area, "HIGH", 0,
            "tram-ext-" + ext.getId(), lat, lon)) {
          count++;
        }
      }
    } catch (Exception e) {
      log.warn("Tram disruption detection failed: {}", e.getMessage());
    }
    return count;
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * Creates and processes a disruption only if no identical one was detected recently (dedup).
   *
   * @return true if a new disruption was created
   */
  private boolean processIfNew(
      String disruptionType,
      String transportMode,
      String affectedArea,
      String severity,
      int delayMinutes,
      String sourceRef) {
    return processIfNewWithCoords(
        disruptionType, transportMode, affectedArea, severity, delayMinutes, sourceRef, null, null);
  }

  @Transactional
  boolean processIfNewWithCoords(
      String disruptionType,
      String transportMode,
      String affectedArea,
      String severity,
      int delayMinutes,
      String sourceRef,
      Double lat,
      Double lon) {

    // Deduplication: skip if same type+area was already detected within the window
    LocalDateTime dedupCutoff = LocalDateTime.now(DUBLIN).minusMinutes(DEDUP_WINDOW_MINUTES);
    List<Disruption> recent =
        disruptionRepository.findByDisruptionTypeAndAffectedAreaAndDetectedAtAfter(
            disruptionType, affectedArea, dedupCutoff);
    if (!recent.isEmpty()) {
      log.debug("Skipping duplicate disruption: type={}, area={}", disruptionType, affectedArea);
      return false;
    }

    // Skip LOW severity unless it's an event (events are always worth recording)
    if ("LOW".equals(severity) && !"EVENT".equals(disruptionType)) {
      log.debug("Skipping LOW severity non-event disruption: area={}", affectedArea);
      return false;
    }

    DisruptionDetectionRequest request =
        buildRequest(
            disruptionType,
            transportMode,
            affectedArea,
            severity,
            delayMinutes,
            sourceRef,
            lat,
            lon);

    try {
      disruptionFacade.handleDisruptionDetection(request);
      log.info(
          "Auto-detected disruption: type={}, severity={}, area={}",
          disruptionType,
          severity,
          affectedArea);
      return true;
    } catch (Exception e) {
      log.error(
          "Failed to process auto-detected disruption for area={}: {}",
          affectedArea,
          e.getMessage());
      return false;
    }
  }

  private DisruptionDetectionRequest buildRequest(
      String disruptionType,
      String transportMode,
      String affectedArea,
      String severity,
      int delayMinutes,
      String sourceRef,
      Double lat,
      Double lon) {

    DisruptionDetectionRequest req = new DisruptionDetectionRequest();
    req.setDisruptionType(disruptionType);
    req.setSeverity(severity);
    req.setAffectedArea(affectedArea);
    req.setDescription(buildDescription(disruptionType, severity, affectedArea, delayMinutes));
    req.setDelayMinutes(delayMinutes > 0 ? delayMinutes : null);
    req.setLatitude(lat);
    req.setLongitude(lon);
    req.setDataSource("SCHEDULED_DETECTION");
    req.setSourceReferenceId(sourceRef);
    req.setEstimatedStartTime(LocalDateTime.now(DUBLIN));
    req.setEstimatedEndTime(LocalDateTime.now(DUBLIN).plusMinutes(estimateEndMinutes(severity)));

    // Parse comma-separated transport modes
    List<String> modes = new ArrayList<>();
    if (transportMode != null) {
      for (String m : transportMode.split(",", -1)) {
        modes.add(m.trim());
      }
    }
    req.setAffectedTransportModes(modes);
    req.setAffectedRoutes(List.of());
    req.setAffectedStops(List.of());

    return req;
  }

  private String buildDescription(String type, String severity, String area, int delayMinutes) {
    return switch (type) {
      case "DELAY" ->
          String.format(
              "%s bus delay detected in %s%s.",
              severity,
              area,
              delayMinutes > 0 ? " — estimated " + delayMinutes + " min average delay" : "");
      case "CONGESTION" -> String.format("%s traffic congestion detected at %s.", severity, area);
      case "EVENT" ->
          String.format("Large event at %s may impact nearby transport services.", area);
      default -> String.format("%s disruption detected in %s.", severity, area);
    };
  }

  private int estimateEndMinutes(String severity) {
    return switch (severity) {
      case "CRITICAL" -> 120;
      case "HIGH" -> 60;
      case "MEDIUM" -> 30;
      default -> 15;
    };
  }
}
