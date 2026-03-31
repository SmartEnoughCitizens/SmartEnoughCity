package com.trinity.hermes.indicators.tram.service;

import com.trinity.hermes.indicators.tram.dto.*;
import com.trinity.hermes.indicators.tram.entity.*;
import com.trinity.hermes.indicators.tram.repository.*;
import java.sql.Time;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TramDashboardService {

  private static final ZoneId DUBLIN_ZONE = ZoneId.of("Europe/Dublin");

  // Daily passenger estimates from CSO TOA11 (54M annual) + TII03 (Red/Green split)
  private static final double RED_DAILY_PASSENGERS = 110_000.0;
  private static final double GREEN_DAILY_PASSENGERS = 80_000.0;

  private final TramStopRepository tramStopRepository;
  private final TramLuasForecastRepository tramLuasForecastRepository;
  private final TramHourlyDistributionRepository tramHourlyDistributionRepository;
  private final TramStopTimeRepository tramStopTimeRepository;
  private final TramGtfsStopRepository tramGtfsStopRepository;
  private final TramDelayHistoryRepository tramDelayHistoryRepository;

  // ── KPIs ────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public TramKpiDTO getKpis() {
    log.info("Fetching tram dashboard KPIs");

    long totalStops = tramStopRepository.countAllStops();
    long activeForecastCount = tramLuasForecastRepository.countAllForecasts();
    long linesOperating = tramStopRepository.countDistinctLines();
    Double avgDueMins = tramLuasForecastRepository.findAverageDueMins();

    return TramKpiDTO.builder()
        .totalStops(totalStops)
        .activeForecastCount(activeForecastCount)
        .linesOperating(linesOperating)
        .avgDueMins(avgDueMins != null ? avgDueMins : 0.0)
        .build();
  }

  // ── Live forecasts ──────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<TramLiveForecastDTO> getLiveForecasts() {
    log.info("Fetching live tram forecasts");

    List<TramLuasForecast> forecasts = tramLuasForecastRepository.findAllOrderedByLineAndStop();
    if (forecasts.isEmpty()) {
      return List.of();
    }

    Map<String, TramStop> stopsById =
        tramStopRepository.findAll().stream()
            .collect(Collectors.toMap(TramStop::getStopId, Function.identity()));

    return forecasts.stream()
        .map(f -> mapToLiveForecastDTO(f, stopsById))
        .collect(Collectors.toList());
  }

  // ── Schedule-based delays with per-stop passenger weighting ─────

  @Transactional(readOnly = true)
  public List<TramDelayDTO> getDelays() {
    log.info("Fetching tram delays (schedule-based)");

    LocalTime nowDublin = LocalTime.now(DUBLIN_ZONE);
    int currentHour = nowDublin.getHour();

    List<TramLuasForecast> forecasts = tramLuasForecastRepository.findAllOrderedByLineAndStop();
    Map<String, TramStop> luasStopsById =
        tramStopRepository.findAll().stream()
            .collect(Collectors.toMap(TramStop::getStopId, Function.identity()));

    Map<String, List<String>> nameToGtfsIds = buildNameToGtfsIdsMap();

    String latestYear = tramHourlyDistributionRepository.findLatestYear();
    Map<String, Double> hourlyPctByLine = getHourlyPctByLine(latestYear, currentHour);

    // Build per-stop trip counts for passenger estimation
    Map<String, int[]> stopDirectionTrips = countTripsAtHourByDirection(currentHour, nameToGtfsIds);
    Map<String, Integer> lineTotalTrips = countLineTotalTripsAtHour(currentHour, luasStopsById, nameToGtfsIds);

    // Group forecasts by stop+direction, keep soonest
    Map<String, TramLuasForecast> soonestPerStopDirection = new HashMap<>();
    for (TramLuasForecast f : forecasts) {
      if (f.getDueMins() == null) {
        continue;
      }
      String key = f.getStopId() + "|" + f.getDirection();
      TramLuasForecast existing = soonestPerStopDirection.get(key);
      if (existing == null || f.getDueMins() < existing.getDueMins()) {
        soonestPerStopDirection.put(key, f);
      }
    }

    List<TramDelayDTO> delays = new ArrayList<>();

    for (TramLuasForecast forecast : soonestPerStopDirection.values()) {
      TramStop luasStop = luasStopsById.get(forecast.getStopId());
      if (luasStop == null) {
        continue;
      }

      String stopNameKey = luasStop.getName().toLowerCase(Locale.ROOT);
      List<String> gtfsStopIds = nameToGtfsIds.get(stopNameKey);
      if (gtfsStopIds == null || gtfsStopIds.isEmpty()) {
        gtfsStopIds = findGtfsStopIdsByPartialName(stopNameKey, nameToGtfsIds);
        if (gtfsStopIds.isEmpty()) {
          continue;
        }
      }

      LocalTime predictedArrival = nowDublin.plusMinutes(forecast.getDueMins());
      LocalTime nextScheduled = findNextScheduledArrival(gtfsStopIds, nowDublin);
      if (nextScheduled == null) {
        continue;
      }

      int delayMins =
          (int) java.time.Duration.between(nextScheduled, predictedArrival).toMinutes();
      if (delayMins <= 0) {
        continue;
      }

      // Per-stop passenger estimate
      int[] dirTrips = stopDirectionTrips.getOrDefault(stopNameKey, new int[] {0, 0});
      int stopTotal = dirTrips[0] + dirTrips[1];
      int lineTotal = lineTotalTrips.getOrDefault(forecast.getLine(), 1);
      double stopShare = (double) stopTotal / Math.max(1, lineTotal);

      String lineKey = "red".equals(forecast.getLine()) ? "-" : "--";
      double hourlyPct = hourlyPctByLine.getOrDefault(lineKey, 0.0) / 100.0;
      double dailyPax =
          "red".equals(forecast.getLine()) ? RED_DAILY_PASSENGERS : GREEN_DAILY_PASSENGERS;
      double estimatedAffected = stopShare * hourlyPct * dailyPax * (delayMins / 60.0);

      delays.add(
          TramDelayDTO.builder()
              .stopId(forecast.getStopId())
              .stopName(luasStop.getName())
              .line(forecast.getLine())
              .direction(forecast.getDirection())
              .destination(forecast.getDestination())
              .scheduledTime(nextScheduled.toString())
              .dueMins(forecast.getDueMins())
              .delayMins(delayMins)
              .estimatedAffectedPassengers(Math.round(estimatedAffected * 10.0) / 10.0)
              .build());
    }

    delays.sort(Comparator.comparingInt(TramDelayDTO::getDelayMins).reversed());
    return delays;
  }

  // ── Stop Usage (per-stop estimated passengers inbound/outbound) ─

  @Transactional(readOnly = true)
  public List<TramStopUsageDTO> getStopUsage(int hour) {
    log.info("Fetching stop usage estimates for hour {}", hour);

    Map<String, TramStop> luasStopsById =
        tramStopRepository.findAll().stream()
            .collect(Collectors.toMap(TramStop::getStopId, Function.identity()));

    Map<String, List<String>> nameToGtfsIds = buildNameToGtfsIdsMap();

    // Get CSO hourly percentage
    String latestYear = tramHourlyDistributionRepository.findLatestYear();
    Map<String, Double> hourlyPctByLine = getHourlyPctByLine(latestYear, hour);

    // Count trips per stop per direction at current hour
    Map<String, int[]> stopDirectionTrips = countTripsAtHourByDirection(hour, nameToGtfsIds);

    // Count total trips per line at selected hour
    Map<String, Integer> lineTotalTrips =
        countLineTotalTripsAtHour(hour, luasStopsById, nameToGtfsIds);

    List<TramStopUsageDTO> usageList = new ArrayList<>();

    for (TramStop luasStop : luasStopsById.values()) {
      String stopNameKey = luasStop.getName().toLowerCase(Locale.ROOT);
      int[] dirTrips = stopDirectionTrips.getOrDefault(stopNameKey, new int[] {0, 0});
      int inboundTrips = dirTrips[1];
      int outboundTrips = dirTrips[0];
      int totalTrips = inboundTrips + outboundTrips;

      // Calculate passenger estimates
      int lineTotal = lineTotalTrips.getOrDefault(luasStop.getLine(), 1);
      String lineKey = "red".equals(luasStop.getLine()) ? "-" : "--";
      double hourlyPct = hourlyPctByLine.getOrDefault(lineKey, 0.0) / 100.0;
      double dailyPax =
          "red".equals(luasStop.getLine()) ? RED_DAILY_PASSENGERS : GREEN_DAILY_PASSENGERS;

      double inboundShare = (double) inboundTrips / Math.max(1, lineTotal);
      double outboundShare = (double) outboundTrips / Math.max(1, lineTotal);

      long estInbound = Math.round(inboundShare * hourlyPct * dailyPax);
      long estOutbound = Math.round(outboundShare * hourlyPct * dailyPax);

      usageList.add(
          TramStopUsageDTO.builder()
              .stopId(luasStop.getStopId())
              .stopName(luasStop.getName())
              .line(luasStop.getLine())
              .currentHour(hour)
              .inboundTrips(inboundTrips)
              .outboundTrips(outboundTrips)
              .totalTrips(totalTrips)
              .estimatedInboundPassengers(estInbound)
              .estimatedOutboundPassengers(estOutbound)
              .estimatedTotalPassengers(estInbound + estOutbound)
              .lat(luasStop.getLat())
              .lon(luasStop.getLon())
              .build());
    }

    usageList.sort(
        Comparator.comparingLong(TramStopUsageDTO::getEstimatedTotalPassengers).reversed());
    return usageList;
  }

  // ── Common delays (historical avg per stop) ─────────────────────

  @Transactional(readOnly = true)
  public List<TramCommonDelayDTO> getCommonDelays() {
    log.info("Fetching common delays from history");

    Map<String, TramStop> luasStopsById =
        tramStopRepository.findAll().stream()
            .collect(Collectors.toMap(TramStop::getStopId, Function.identity()));

    List<Object[]> rows = tramDelayHistoryRepository.findAvgDelayPerStop();
    List<TramCommonDelayDTO> result = new ArrayList<>();

    for (Object[] row : rows) {
      String stopId = (String) row[0];
      String stopName = (String) row[1];
      String line = (String) row[2];
      double avgDelay = ((Number) row[3]).doubleValue();
      int maxDelay = ((Number) row[4]).intValue();
      long count = ((Number) row[5]).longValue();

      TramStop stop = luasStopsById.get(stopId);

      result.add(
          TramCommonDelayDTO.builder()
              .stopId(stopId)
              .stopName(stopName)
              .line(line)
              .avgDelayMins(avgDelay)
              .maxDelayMins(maxDelay)
              .delayCount(count)
              .lat(stop != null ? stop.getLat() : null)
              .lon(stop != null ? stop.getLon() : null)
              .build());
    }

    return result;
  }

  // ── Shared helpers ──────────────────────────────────────────────

  private Map<String, List<String>> buildNameToGtfsIdsMap() {
    Map<String, List<String>> nameToGtfsIds = new HashMap<>();
    for (TramGtfsStop gs : tramGtfsStopRepository.findAll()) {
      nameToGtfsIds
          .computeIfAbsent(gs.getName().toLowerCase(Locale.ROOT), k -> new ArrayList<>())
          .add(gs.getId());
    }
    return nameToGtfsIds;
  }

  private Map<String, Double> getHourlyPctByLine(String latestYear, int currentHour) {
    if (latestYear == null) {
      return Map.of();
    }
    return tramHourlyDistributionRepository.findByYear(latestYear).stream()
        .filter(h -> parseHour(h.getTimeLabel()) == currentHour)
        .collect(
            Collectors.toMap(
                TramHourlyDistribution::getLineCode,
                h -> h.getValue() != null ? h.getValue() : 0.0,
                (a, b) -> a));
  }

  /**
   * Count trips per stop (by Luas stop name) per direction at a given hour. Returns map of
   * stopNameLowerCase -> [outbound_count, inbound_count].
   */
  private Map<String, int[]> countTripsAtHourByDirection(
      int hour, Map<String, List<String>> nameToGtfsIds) {
    // Build reverse map: gtfsStopId -> stopNameLowerCase
    Map<String, String> gtfsIdToName = new HashMap<>();
    for (Map.Entry<String, List<String>> entry : nameToGtfsIds.entrySet()) {
      for (String gid : entry.getValue()) {
        gtfsIdToName.put(gid, entry.getKey());
      }
    }

    // Count all trips at this stop at this hour
    // Split by GTFS stop_id pairs (each stop has 2 platform IDs, one per direction)
    Map<String, int[]> result = new HashMap<>();

    List<TramStopTime> allStopTimes = tramStopTimeRepository.findAll();
    for (TramStopTime st : allStopTimes) {
      if (st.getArrivalTime() == null) {
        continue;
      }
      int arrHour = st.getArrivalTime().toLocalTime().getHour();
      if (arrHour != hour) {
        continue;
      }

      String stopName = gtfsIdToName.get(st.getStopId());
      if (stopName == null) {
        continue;
      }

      // Each stop has 2 GTFS IDs (one per platform/direction)
      // The first ID in the sorted list is typically outbound, second is inbound
      List<String> ids = nameToGtfsIds.get(stopName);
      int dirIdx = 0; // default outbound
      if (ids != null && ids.size() > 1) {
        dirIdx = st.getStopId().equals(ids.get(0)) ? 0 : 1;
      }

      int[] counts = result.computeIfAbsent(stopName, k -> new int[] {0, 0});
      counts[dirIdx]++;
    }

    return result;
  }

  private Map<String, Integer> countLineTotalTripsAtHour(
      int hour,
      Map<String, TramStop> luasStopsById,
      Map<String, List<String>> nameToGtfsIds) {

    // Map GTFS stop IDs to line name
    Map<String, String> gtfsStopToLine = new HashMap<>();
    for (TramStop luasStop : luasStopsById.values()) {
      String nameKey = luasStop.getName().toLowerCase(Locale.ROOT);
      List<String> ids = nameToGtfsIds.get(nameKey);
      if (ids == null) {
        ids = findGtfsStopIdsByPartialName(nameKey, nameToGtfsIds);
      }
      if (ids != null) {
        for (String gid : ids) {
          gtfsStopToLine.put(gid, luasStop.getLine());
        }
      }
    }

    Map<String, Integer> lineTotals = new HashMap<>();
    List<TramStopTime> allStopTimes = tramStopTimeRepository.findAll();
    for (TramStopTime st : allStopTimes) {
      if (st.getArrivalTime() == null) {
        continue;
      }
      int arrHour = st.getArrivalTime().toLocalTime().getHour();
      if (arrHour != hour) {
        continue;
      }
      String line = gtfsStopToLine.getOrDefault(st.getStopId(), "unknown");
      lineTotals.merge(line, 1, Integer::sum);
    }
    return lineTotals;
  }

  private LocalTime findNextScheduledArrival(List<String> gtfsStopIds, LocalTime now) {
    Time sqlNow = Time.valueOf(now);
    Time sqlEnd = Time.valueOf(now.plusMinutes(90));
    LocalTime nearest = null;

    for (String gtfsStopId : gtfsStopIds) {
      List<TramStopTime> stopTimes =
          tramStopTimeRepository.findByStopIdOrderByArrivalTime(gtfsStopId);
      for (TramStopTime st : stopTimes) {
        Time arr = st.getArrivalTime();
        if (arr.compareTo(sqlNow) >= 0 && arr.compareTo(sqlEnd) <= 0) {
          LocalTime arrLocal = arr.toLocalTime();
          if (nearest == null || arrLocal.isBefore(nearest)) {
            nearest = arrLocal;
          }
          break;
        }
      }
    }
    return nearest;
  }

  private List<String> findGtfsStopIdsByPartialName(
      String stopName, Map<String, List<String>> nameToGtfsIds) {
    for (Map.Entry<String, List<String>> entry : nameToGtfsIds.entrySet()) {
      String gtfsName = entry.getKey();
      if (gtfsName.contains(stopName) || stopName.contains(gtfsName)) {
        return entry.getValue();
      }
    }
    return List.of();
  }

  // ── Hourly distribution ─────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<TramHourlyDistributionDTO> getHourlyDistribution() {
    log.info("Fetching tram hourly passenger distribution");

    String latestYear = tramHourlyDistributionRepository.findLatestYear();
    if (latestYear == null) {
      return List.of();
    }

    return tramHourlyDistributionRepository.findByYear(latestYear).stream()
        .map(this::mapToHourlyDTO)
        .collect(Collectors.toList());
  }

  // ── Stations list ───────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<TramStopDTO> getStops(int limit) {
    log.debug("Fetching up to {} tram stops", limit);
    return tramStopRepository.findAll().stream()
        .limit(limit)
        .map(this::mapToStopDTO)
        .collect(Collectors.toList());
  }

  // ── Mapping helpers ─────────────────────────────────────────────

  private TramLiveForecastDTO mapToLiveForecastDTO(
      TramLuasForecast forecast, Map<String, TramStop> stopsById) {
    TramStop stop = stopsById.get(forecast.getStopId());
    return TramLiveForecastDTO.builder()
        .stopId(forecast.getStopId())
        .stopName(stop != null ? stop.getName() : forecast.getStopId())
        .line(forecast.getLine())
        .direction(forecast.getDirection())
        .destination(forecast.getDestination())
        .dueMins(forecast.getDueMins())
        .message(forecast.getMessage())
        .lat(stop != null ? stop.getLat() : null)
        .lon(stop != null ? stop.getLon() : null)
        .build();
  }

  private TramHourlyDistributionDTO mapToHourlyDTO(TramHourlyDistribution entity) {
    return TramHourlyDistributionDTO.builder()
        .timeLabel(entity.getTimeLabel())
        .line(entity.getLineLabel())
        .percentage(entity.getValue())
        .build();
  }

  private TramStopDTO mapToStopDTO(TramStop entity) {
    TramStopDTO dto = new TramStopDTO();
    dto.setStopId(entity.getStopId());
    dto.setLine(entity.getLine());
    dto.setName(entity.getName());
    dto.setLat(entity.getLat());
    dto.setLon(entity.getLon());
    dto.setParkRide(entity.getParkRide());
    dto.setCycleRide(entity.getCycleRide());
    return dto;
  }

  private int parseHour(String timeLabel) {
    try {
      return Integer.parseInt(timeLabel.substring(0, 2).trim());
    } catch (Exception e) {
      return -1;
    }
  }
}
