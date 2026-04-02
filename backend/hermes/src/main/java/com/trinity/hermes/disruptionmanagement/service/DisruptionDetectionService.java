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
import com.trinity.hermes.indicators.tram.entity.TramLuasForecast;
import com.trinity.hermes.indicators.tram.entity.TramStop;
import com.trinity.hermes.indicators.tram.repository.TramLuasForecastRepository;
import com.trinity.hermes.indicators.tram.repository.TramStopRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
  private static final int LARGE_EVENT_VENUE_CAPACITY_THRESHOLD = 1000; // venue capacity
  private static final int DEDUP_WINDOW_MINUTES = 10; // ignore same disruption within 10 min
  private static final int TRAIN_LATE_THRESHOLD_MINUTES = 10; // lateMinutes > 10
  private static final int TRAIN_MIN_LATE_TRAINS = 3; // ≥3 late trains at same station
  private static final int TRAM_PEAK_FREQ_MINS = 5; // Luas peak: every ~5 min
  private static final int TRAM_OFFPEAK_FREQ_MINS = 10; // off-peak: every ~10 min
  private static final int TRAM_LATE_FREQ_MINS = 15; // late night: every ~15 min
  private static final int TRAM_DISRUPTION_THRESHOLD_MINS = 10; // delay above expected freq to fire

  private final BusRouteMetricsRepository busRouteMetricsRepository;
  private final HighTrafficPointsRepository highTrafficPointsRepository;
  private final EventsRepository eventsRepository;
  private final DisruptionRepository disruptionRepository;
  private final DisruptionFacade disruptionFacade;
  private final TrainStationDataRepository trainStationDataRepository;
  private final TrainStationRepository trainStationRepository;
  private final TramLuasForecastRepository tramLuasForecastRepository;
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
          eventsRepository.findUpcomingEventsAtLargeVenues(
              LARGE_EVENT_VENUE_CAPACITY_THRESHOLD,
              org.springframework.data.domain.PageRequest.of(0, 20));

      for (com.trinity.hermes.indicators.events.entity.Events ev : events) {
        int capacity = ev.getVenue().getCapacity();
        String area = ev.getVenueName() != null ? ev.getVenueName() : "Dublin";
        // Severity by venue capacity:
        //   CRITICAL ≥ 15000  (stadium-scale: Aviva, 3Arena, Croke Park)
        //   HIGH     ≥ 5000   (large: RDS, Marlay Park)
        //   MEDIUM   ≥ 2500   (mid-size: Bord Gáis, Gaiety)
        //   LOW      ≥ 1000   (small: Vicar Street, Olympia, Ambassador)
        String severity;
        if (capacity >= 15000) {
          severity = "CRITICAL";
        } else if (capacity >= 5000) {
          severity = "HIGH";
        } else if (capacity >= 2500) {
          severity = "MEDIUM";
        } else {
          severity = "LOW";
        }

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
            entry.getValue().stream().mapToInt(TrainStationData::getLateMinutes).max().orElse(0);
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

        if (processIfNewWithCoords(
            "DELAY", "TRAIN", area, severity, maxDelay, stationCode, lat, lon)) {
          count++;
        }
      }
    } catch (Exception e) {
      log.warn("Train disruption detection failed: {}", e.getMessage());
    }
    return count;
  }

  // ---------------------------------------------------------------------------
  // TRAM — detect delays directly from live tram_luas_forecasts
  // ---------------------------------------------------------------------------

  private int detectTramDisruptions() {
    int count = 0;
    try {
      int hour = LocalTime.now(DUBLIN).getHour();
      // Luas doesn't run between 01:00 and 05:59 — skip to avoid false positives
      if (hour >= 1 && hour < 6) return 0;

      List<TramLuasForecast> allForecasts = tramLuasForecastRepository.findAll();
      // If no forecasts at all, data hasn't been loaded yet — don't fire
      if (allForecasts.isEmpty()) return 0;

      int expectedFreq = getTramExpectedFrequency(hour);

      // Group forecasts by stop, then find the minimum due_mins per stop
      Map<String, List<TramLuasForecast>> byStop =
          allForecasts.stream().collect(Collectors.groupingBy(TramLuasForecast::getStopId));

      // Build stop lookup for lat/lon and name
      Map<String, TramStop> stopMap =
          tramStopRepository.findAll().stream()
              .collect(Collectors.toMap(TramStop::getStopId, s -> s, (a, b) -> a));

      for (Map.Entry<String, List<TramLuasForecast>> entry : byStop.entrySet()) {
        String stopId = entry.getKey();

        // Find the soonest tram at this stop
        java.util.OptionalInt minDue =
            entry.getValue().stream()
                .filter(f -> f.getDueMins() != null)
                .mapToInt(TramLuasForecast::getDueMins)
                .min();

        if (minDue.isEmpty()) continue;

        int delayMins = minDue.getAsInt() - expectedFreq;
        if (delayMins < TRAM_DISRUPTION_THRESHOLD_MINS) continue;

        TramStop stop = stopMap.get(stopId);
        Double lat = stop != null ? stop.getLat() : null;
        Double lon = stop != null ? stop.getLon() : null;
        String area = stop != null ? stop.getName() : "Tram Stop " + stopId;
        String severity = delayMins > 20 ? "HIGH" : "MEDIUM";

        if (processIfNewWithCoords(
            "TRAM_DISRUPTION", "TRAM", area, severity, delayMins, "tram-" + stopId, lat, lon)) {
          count++;
        }
      }
    } catch (Exception e) {
      log.warn("Tram disruption detection failed: {}", e.getMessage());
    }
    return count;
  }

  private int getTramExpectedFrequency(int hour) {
    if ((hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 19)) return TRAM_PEAK_FREQ_MINS;
    if (hour >= 10 && hour <= 15) return TRAM_OFFPEAK_FREQ_MINS;
    return TRAM_LATE_FREQ_MINS;
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

    // Primary guard: never open a new record while an ACTIVE one already exists for the same
    // type + area. This prevents repeated notifications for persistent disruptions (e.g. a bus
    // route that stays delayed across many 5-min scheduler cycles).
    if (disruptionRepository.existsByDisruptionTypeAndAffectedAreaAndStatus(
        disruptionType, affectedArea, "ACTIVE")) {
      log.debug(
          "Skipping: active disruption already exists: type={}, area={}",
          disruptionType,
          affectedArea);
      return false;
    }

    // Secondary guard: dedup window catches the brief gap while a record is transitioning from
    // DETECTED/ANALYZING to ACTIVE (i.e. not yet visible to the primary guard above).
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
