package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.common.Constants;
import com.trinity.hermes.disruptionmanagement.dto.DisruptionDetectionRequest;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.facade.DisruptionFacade;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionRepository;
import com.trinity.hermes.indicators.bus.repository.BusLiveStopTimeUpdateRepository;
import com.trinity.hermes.indicators.bus.repository.BusStopRepository;
import com.trinity.hermes.indicators.car.repository.HighTrafficPointsRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service that detects live transport disruptions every 5 minutes.
 *
 * <p>A disruption is a real, current event affecting passengers: a vehicle running 30+ minutes late
 * at a stop, or active road congestion whose location overlaps with bus/tram routes. Events and
 * planned service-pressure scenarios are handled separately by {@link ServicePressureService}.
 *
 * <p>Detection logic per mode:
 *
 * <ul>
 *   <li><b>Bus</b> — worst arrival delay per route ≥ 30 min; anchored to the stop location.
 *   <li><b>Train</b> — ≥3 trains late > 10 min at the same station.
 *   <li><b>Tram</b> — soonest due_mins at a stop exceeds expected frequency + 10 min threshold.
 *   <li><b>Congestion</b> — high-volume traffic site whose radius overlaps bus routes or tram
 *       stops; buses get a rerouting recommendation, trams get a warning-only notification.
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisruptionDetectionService {

  private static final ZoneId DUBLIN = ZoneId.of("Europe/Dublin");

  // Bus: only fire when a stop has ≥ 30 min arrival delay
  private static final int BUS_STOP_DELAY_THRESHOLD_SECONDS = 1800;

  // Congestion: fire when peak avg volume at a site exceeds this threshold
  private static final long HIGH_TRAFFIC_VOLUME_THRESHOLD = 1500L;

  // Radius to scan for affected bus routes / tram stops around a congestion site
  private static final int CONGESTION_SCAN_RADIUS_M = 500;

  // Train: ≥ 3 trains delayed > 10 min at the same station
  private static final int TRAIN_LATE_THRESHOLD_MINUTES = 10;
  private static final int TRAIN_MIN_LATE_TRAINS = 3;

  // Tram: expected service frequency by time of day (Luas schedule)
  private static final int TRAM_PEAK_FREQ_MINS = 5;
  private static final int TRAM_OFFPEAK_FREQ_MINS = 10;
  private static final int TRAM_LATE_FREQ_MINS = 15;
  private static final int TRAM_DISRUPTION_THRESHOLD_MINS = 10;

  private final BusLiveStopTimeUpdateRepository busLiveStopTimeUpdateRepository;
  private final BusStopRepository busStopRepository;
  private final HighTrafficPointsRepository highTrafficPointsRepository;
  private final DisruptionRepository disruptionRepository;
  private final DisruptionFacade disruptionFacade;
  private final TrainStationDataRepository trainStationDataRepository;
  private final TrainStationRepository trainStationRepository;
  private final TramLuasForecastRepository tramLuasForecastRepository;
  private final TramStopRepository tramStopRepository;

  // ---------------------------------------------------------------------------
  // Main scheduled cycle
  // ---------------------------------------------------------------------------

  @Scheduled(fixedRate = 300_000, initialDelay = 15_000)
  public void detectDisruptions() {
    log.info("=== DISRUPTION AUTO-DETECTION CYCLE STARTED ===");
    int detected = 0;

    int expired = autoResolveExpiredDisruptions();
    if (expired > 0) log.info("Auto-resolved {} expired disruption(s)", expired);

    detected += detectBusDisruptions();
    detected += detectCongestionDisruptions();
    detected += detectTrainDisruptions();
    detected += detectTramDisruptions();

    log.info("=== DISRUPTION AUTO-DETECTION CYCLE COMPLETE: {} new disruption(s) ===", detected);
  }

  // ---------------------------------------------------------------------------
  // AUTO-EXPIRY
  // ---------------------------------------------------------------------------

  @Transactional
  int autoResolveExpiredDisruptions() {
    LocalDateTime now = LocalDateTime.now(DUBLIN);
    List<Disruption> expired = disruptionRepository.findExpiredActiveDisruptions(now);
    for (Disruption d : expired) {
      d.setStatus("RESOLVED");
      d.setResolvedAt(now);
      disruptionRepository.save(d);
      log.debug("Auto-resolved expired disruption id={} area={}", d.getId(), d.getAffectedArea());
    }
    return expired.size();
  }

  // ---------------------------------------------------------------------------
  // BUS — stops with ≥ 30 min arrival delay
  // ---------------------------------------------------------------------------

  private int detectBusDisruptions() {
    int count = 0;
    try {
      // Prefer the pre-aggregated MV (refreshed every 2 min); fall back to the live join if the MV
      // has not been populated yet (first boot before MvBootstrap completes).
      List<Object[]> rows;
      try {
        rows = busLiveStopTimeUpdateRepository.findWorstDelayedStopPerRouteFromMv();
      } catch (Exception mvEx) {
        log.debug("MV not ready, falling back to live bus delay query: {}", mvEx.getMessage());
        rows =
            busLiveStopTimeUpdateRepository.findWorstDelayedStopPerRoute(
                BUS_STOP_DELAY_THRESHOLD_SECONDS,
                Constants.DUBLIN_LAT_MIN,
                Constants.DUBLIN_LAT_MAX,
                Constants.DUBLIN_LON_MIN,
                Constants.DUBLIN_LON_MAX);
      }

      // Group rows by stop_id so one disruption is created per stop (all affected routes combined)
      Map<String, List<Object[]>> byStop = new LinkedHashMap<>();
      for (Object[] row : rows) {
        if (row.length < 6) continue;
        String stopId = row[1] != null ? row[1].toString() : "unknown";
        byStop.computeIfAbsent(stopId, k -> new ArrayList<>()).add(row);
      }

      for (Map.Entry<String, List<Object[]>> entry : byStop.entrySet()) {
        String stopId = entry.getKey();
        List<Object[]> stopRows = entry.getValue();

        List<String> routes =
            stopRows.stream()
                .map(r -> r[0] != null ? r[0].toString() : null)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Object[] first = stopRows.get(0);
        String stopName = first[2] != null ? first[2].toString() : "Bus Stop";
        Double lat = first[3] != null ? ((Number) first[3]).doubleValue() : null;
        Double lon = first[4] != null ? ((Number) first[4]).doubleValue() : null;

        int maxDelaySec =
            stopRows.stream()
                .filter(r -> r[5] != null)
                .mapToInt(r -> ((Number) r[5]).intValue())
                .max()
                .orElse(0);

        int delayMinutes = maxDelaySec / 60;
        String severity = scoreBusStopSeverity(maxDelaySec);
        String sourceRef = "bus-stop-" + stopId;

        String description = buildBusStopDescription(routes, delayMinutes, stopName);

        if (processIfNewWithCoords(
            "DELAY",
            "BUS",
            stopName,
            severity,
            delayMinutes,
            sourceRef,
            lat,
            lon,
            routes,
            List.of(),
            description)) {
          count++;
        }
      }
    } catch (Exception e) {
      log.warn("Bus disruption detection failed: {}", e.getMessage());
    }
    return count;
  }

  private String buildBusStopDescription(List<String> routes, int delayMinutes, String stopName) {
    String routePart =
        routes.size() == 1 ? "Route " + routes.get(0) : "Routes " + String.join(", ", routes);
    return String.format("%s running ~%d min late at %s.", routePart, delayMinutes, stopName);
  }

  private String scoreBusStopSeverity(int delaySec) {
    if (delaySec > 3600) return "CRITICAL"; // > 60 min
    if (delaySec > 2700) return "HIGH"; // > 45 min
    return "MEDIUM"; // > 30 min (threshold)
  }

  // ---------------------------------------------------------------------------
  // CONGESTION — live high-traffic sites overlapping bus routes or tram stops
  //
  // When congestion is detected:
  //   Bus routes passing through → rerouting recommendation sent to operators
  //   Tram stops within radius   → warning-only notification (trams can't reroute)
  //   Nearby cycle docks         → suggested as passenger alternative (via
  // AlternativeTransportService)
  // ---------------------------------------------------------------------------

  private int detectCongestionDisruptions() {
    int count = 0;
    try {
      // Returns [site_id, lat, lon, max_volume] — one row per site
      List<Object[]> rows;
      try {
        rows = highTrafficPointsRepository.findPeakTrafficSitesFromMv();
      } catch (Exception mvEx) {
        log.debug("MV not ready, falling back to live traffic query: {}", mvEx.getMessage());
        rows = highTrafficPointsRepository.findPeakTrafficSitesWithLocation();
      }

      for (Object[] row : rows) {
        if (row.length < 4) continue;
        long volume = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        if (volume < HIGH_TRAFFIC_VOLUME_THRESHOLD) continue;

        String siteId = row[0] != null ? row[0].toString() : "unknown";
        Double lat = row[1] != null ? ((Number) row[1]).doubleValue() : null;
        Double lon = row[2] != null ? ((Number) row[2]).doubleValue() : null;
        if (lat == null || lon == null) continue;

        String severity = volume > HIGH_TRAFFIC_VOLUME_THRESHOLD * 2 ? "HIGH" : "MEDIUM";

        // Find affected bus routes and tram lines within the scan radius
        List<String> busRoutes = findBusRoutesNear(lat, lon, CONGESTION_SCAN_RADIUS_M);
        List<String> tramLines = findTramLinesNear(lat, lon, CONGESTION_SCAN_RADIUS_M);

        if (busRoutes.isEmpty() && tramLines.isEmpty()) {
          log.debug("Congestion at site {} has no nearby transport — skipping", siteId);
          continue;
        }

        List<String> modes = new ArrayList<>();
        if (!busRoutes.isEmpty()) modes.add("BUS");
        if (!tramLines.isEmpty()) modes.add("TRAM");

        String area = "Traffic Site " + siteId;
        String description = buildCongestionDescription(siteId, busRoutes, tramLines);

        if (processIfNewWithCoords(
            "CONGESTION",
            String.join(",", modes),
            area,
            severity,
            0,
            siteId,
            lat,
            lon,
            busRoutes,
            tramLines,
            description)) {
          count++;
        }
      }
    } catch (Exception e) {
      log.warn("Congestion disruption detection failed: {}", e.getMessage());
    }
    return count;
  }

  // ---------------------------------------------------------------------------
  // TRAIN — ≥ 3 trains late > 10 min at the same station
  // ---------------------------------------------------------------------------

  private int detectTrainDisruptions() {
    int count = 0;
    try {
      // MV returns [station_code, late_minutes]; fallback converts entity list to same shape
      List<Object[]> rows;
      try {
        rows = trainStationDataRepository.findLatestPerStationTrainFromMv();
      } catch (Exception mvEx) {
        log.debug("MV not ready, falling back to live train query: {}", mvEx.getMessage());
        rows =
            trainStationDataRepository
                .findLatestPerStationTrain(
                    Constants.DUBLIN_LAT_MIN,
                    Constants.DUBLIN_LAT_MAX,
                    Constants.DUBLIN_LON_MIN,
                    Constants.DUBLIN_LON_MAX)
                .stream()
                .<Object[]>map(sd -> new Object[] {sd.getStationCode(), sd.getLateMinutes()})
                .collect(Collectors.toList());
      }

      Map<String, List<Integer>> byStation = new LinkedHashMap<>();
      for (Object[] r : rows) {
        if (r.length < 2) continue;
        String code = r[0] != null ? r[0].toString() : null;
        Integer lateMin = r[1] instanceof Number n ? n.intValue() : null;
        if (code == null || lateMin == null || lateMin <= TRAIN_LATE_THRESHOLD_MINUTES) continue;
        byStation.computeIfAbsent(code, k -> new ArrayList<>()).add(lateMin);
      }

      for (Map.Entry<String, List<Integer>> entry : byStation.entrySet()) {
        if (entry.getValue().size() < TRAIN_MIN_LATE_TRAINS) continue;

        String stationCode = entry.getKey();
        int maxDelay = entry.getValue().stream().mapToInt(Integer::intValue).max().orElse(0);
        String severity = maxDelay > 30 ? "HIGH" : maxDelay > 20 ? "MEDIUM" : "LOW";

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
  // TRAM — soonest tram at stop is overdue relative to expected frequency
  // ---------------------------------------------------------------------------

  private int detectTramDisruptions() {
    int count = 0;
    try {
      int hour = LocalTime.now(DUBLIN).getHour();
      // Luas doesn't run between 01:00–05:59 — skip to avoid false positives
      if (hour >= 1 && hour < 6) return 0;

      List<TramLuasForecast> allForecasts = tramLuasForecastRepository.findAll();
      if (allForecasts.isEmpty()) return 0;

      int expectedFreq = getTramExpectedFrequency(hour);

      Map<String, List<TramLuasForecast>> byStop =
          allForecasts.stream().collect(Collectors.groupingBy(TramLuasForecast::getStopId));

      Map<String, TramStop> stopMap =
          tramStopRepository.findAll().stream()
              .collect(Collectors.toMap(TramStop::getStopId, s -> s, (a, b) -> a));

      for (Map.Entry<String, List<TramLuasForecast>> entry : byStop.entrySet()) {
        String stopId = entry.getKey();

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
  // Spatial helpers — find affected transport near a location
  // ---------------------------------------------------------------------------

  /** Returns distinct bus route short names with stops within {@code radiusM} metres. */
  List<String> findBusRoutesNear(double lat, double lon, int radiusM) {
    try {
      return busStopRepository.findRouteShortNamesNear(lat, lon, radiusM).stream()
          .map(r -> r[1] != null ? r[1].toString() : null)
          .filter(Objects::nonNull)
          .distinct()
          .sorted()
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.warn("Bus route lookup failed near ({}, {}): {}", lat, lon, e.getMessage());
      return List.of();
    }
  }

  /** Returns distinct Luas line names with stops within {@code radiusM} metres. */
  List<String> findTramLinesNear(double lat, double lon, int radiusM) {
    try {
      return tramStopRepository.findStopsNear(lat, lon, radiusM).stream()
          .map(r -> r[1] != null ? r[1].toString() : null)
          .filter(Objects::nonNull)
          .distinct()
          .sorted()
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.warn("Tram stop lookup failed near ({}, {}): {}", lat, lon, e.getMessage());
      return List.of();
    }
  }

  // ---------------------------------------------------------------------------
  // Dedup + request dispatch
  // ---------------------------------------------------------------------------

  /**
   * Full version used for congestion: pre-computed routes, stops, and description are passed
   * through to the detection request.
   */
  @Transactional
  boolean processIfNewWithCoords(
      String disruptionType,
      String transportMode,
      String affectedArea,
      String severity,
      int delayMinutes,
      String sourceRef,
      Double lat,
      Double lon,
      List<String> affectedRoutes,
      List<String> affectedStops,
      String description) {

    if ("LOW".equals(severity)) {
      log.debug("Skipping LOW severity disruption: area={}", affectedArea);
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
            lon,
            affectedRoutes,
            affectedStops,
            description);

    try {
      com.trinity.hermes.disruptionmanagement.dto.DisruptionSolution solution =
          disruptionFacade.handleDisruptionDetection(request);
      if (solution != null) {
        log.info(
            "Auto-detected disruption: type={}, severity={}, area={}",
            disruptionType,
            severity,
            affectedArea);
        return true;
      }
      return false;
    } catch (Exception e) {
      log.error(
          "Failed to process auto-detected disruption for area={}: {}",
          affectedArea,
          e.getMessage());
      return false;
    }
  }

  /** Convenience overload for delay disruptions (no pre-computed routes/stops). */
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
    return processIfNewWithCoords(
        disruptionType,
        transportMode,
        affectedArea,
        severity,
        delayMinutes,
        sourceRef,
        lat,
        lon,
        List.of(),
        List.of(),
        null);
  }

  private DisruptionDetectionRequest buildRequest(
      String disruptionType,
      String transportMode,
      String affectedArea,
      String severity,
      int delayMinutes,
      String sourceRef,
      Double lat,
      Double lon,
      List<String> affectedRoutes,
      List<String> affectedStops,
      String description) {

    DisruptionDetectionRequest req = new DisruptionDetectionRequest();
    req.setDisruptionType(disruptionType);
    req.setSeverity(severity);
    req.setAffectedArea(affectedArea);
    req.setDescription(
        description != null
            ? description
            : buildDescription(disruptionType, affectedArea, delayMinutes));
    req.setDelayMinutes(delayMinutes > 0 ? delayMinutes : null);
    req.setLatitude(lat);
    req.setLongitude(lon);
    req.setDataSource("SCHEDULED_DETECTION");
    req.setSourceReferenceId(sourceRef);
    req.setEstimatedStartTime(LocalDateTime.now(DUBLIN));
    req.setEstimatedEndTime(LocalDateTime.now(DUBLIN).plusMinutes(estimateEndMinutes(severity)));

    List<String> modes = new ArrayList<>();
    if (transportMode != null) {
      for (String m : transportMode.split(",", -1)) {
        modes.add(m.trim());
      }
    }
    req.setAffectedTransportModes(modes);
    req.setAffectedRoutes(new ArrayList<>(affectedRoutes));
    req.setAffectedStops(new ArrayList<>(affectedStops));
    req.setStopId(deriveStopId(sourceRef));

    return req;
  }

  private String deriveStopId(String sourceRef) {
    if (sourceRef == null) return null;
    if (sourceRef.startsWith("bus-stop-")) return sourceRef.substring("bus-stop-".length());
    if (sourceRef.startsWith("tram-")) return sourceRef.substring("tram-".length());
    return sourceRef; // train station code or traffic site ID used as-is
  }

  // ---------------------------------------------------------------------------
  // Description builders
  // ---------------------------------------------------------------------------

  private String buildDescription(String type, String area, int delayMinutes) {
    return switch (type) {
      case "DELAY" ->
          delayMinutes > 0
              ? String.format(
                  "Service delays of approximately %d minutes reported at %s. "
                      + "Passengers should allow extra travel time or consider alternatives.",
                  delayMinutes, area)
              : String.format(
                  "Service delays reported at %s. "
                      + "Passengers should allow extra travel time or consider alternatives.",
                  area);
      case "TRAM_DISRUPTION" ->
          String.format(
              "Luas service at %s is running approximately %d minutes behind schedule. "
                  + "Passengers should check real-time displays or allow additional journey time.",
              area, delayMinutes);
      default ->
          String.format(
              "A transport disruption has been detected at %s. "
                  + "Passengers should check for service updates.",
              area);
    };
  }

  private String buildCongestionDescription(
      String siteId, List<String> busRoutes, List<String> tramLines) {
    StringBuilder sb = new StringBuilder();
    sb.append("High traffic congestion at site ").append(siteId).append(".");
    if (!busRoutes.isEmpty()) {
      sb.append(" Bus routes ")
          .append(String.join(", ", busRoutes))
          .append(" are affected — rerouting recommended.");
    }
    if (!tramLines.isEmpty()) {
      sb.append(" Luas ")
          .append(String.join("/", tramLines))
          .append(" service delays likely — warning issued to operator.");
    }
    sb.append(" Nearby cycle docks may offer a passenger alternative.");
    return sb.toString();
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
