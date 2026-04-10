package com.trinity.hermes.indicators.tram.service;

import com.trinity.hermes.disruptionmanagement.service.AlternativeTransportService;
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
  private static final double RED_DAILY_PASSENGERS = 110_000.0;
  private static final double GREEN_DAILY_PASSENGERS = 80_000.0;

  private final TramStopRepository tramStopRepository;
  private final TramLuasForecastRepository tramLuasForecastRepository;
  private final TramHourlyDistributionRepository tramHourlyDistributionRepository;
  private final TramStopTimeRepository tramStopTimeRepository;
  private final TramGtfsStopRepository tramGtfsStopRepository;
  private final TramDelayHistoryRepository tramDelayHistoryRepository;
  private final AlternativeTransportService alternativeTransportService;

  // ── KPIs ────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public TramKpiDTO getKpis() {
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

  @Transactional(readOnly = true)
  public List<TramLiveForecastDTO> getLiveForecasts() {
    List<TramLuasForecast> forecasts = tramLuasForecastRepository.findAllOrderedByLineAndStop();
    if (forecasts.isEmpty()) return List.of();
    Map<String, TramStop> stopsById = buildLuasStopsMap();
    return forecasts.stream()
        .map(f -> mapToLiveForecastDTO(f, stopsById))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<TramAlternativeRouteDTO> getAlternativeRoutes(String stopId) {
    List<TramLuasForecast> forecasts = tramLuasForecastRepository.findByStopId(stopId);
    boolean isDisrupted = forecasts.stream().anyMatch(f -> isDisruptionMessage(f.getMessage()));

    if (!isDisrupted) return List.of();

    TramStop stop =
        tramStopRepository.findAll().stream()
            .filter(s -> s.getStopId().equals(stopId))
            .findFirst()
            .orElse(null);

    if (stop == null || stop.getLat() == null || stop.getLon() == null) {
      return List.of();
    }

    return alternativeTransportService.findNearby(stop.getLat(), stop.getLon()).stream()
        .map(
            r ->
                TramAlternativeRouteDTO.builder()
                    .transportType(r.transportType())
                    .stopId(r.stopId())
                    .stopName(r.stopName())
                    .lat(r.lat())
                    .lon(r.lon())
                    .distanceM(r.distanceM())
                    .availableBikes(r.availableBikes())
                    .capacity(r.capacity())
                    .build())
        .collect(Collectors.toList());
  }

  private boolean isDisruptionMessage(String message) {
    if (message == null) return false;
    String lower = message.toLowerCase(Locale.ROOT);
    return lower.contains("not in service")
        || lower.contains("disruption")
        || lower.contains("suspended")
        || lower.contains("delay")
        || lower.contains("fault")
        || lower.contains("no service")
        || lower.contains("terminated");
  }

  // ── Delays ──────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<TramDelayDTO> getDelays() {
    LocalTime nowDublin = LocalTime.now(DUBLIN_ZONE);
    int currentHour = nowDublin.getHour();
    List<TramLuasForecast> forecasts = tramLuasForecastRepository.findAllOrderedByLineAndStop();
    Map<String, TramStop> luasStopsById = buildLuasStopsMap();
    Map<String, List<String>> nameToGtfsIds = buildNameToGtfsIdsMap();
    Map<String, String> luasToGtfs = buildLuasToGtfsNameMap(luasStopsById, nameToGtfsIds);
    Map<String, Double> hourlyPct = getHourlyPctForHours(List.of(currentHour));
    List<TramStopTime> allStopTimes = tramStopTimeRepository.findAll();
    Map<String, int[]> stopDirTrips =
        countTripsForHours(List.of(currentHour), nameToGtfsIds, allStopTimes);
    Map<String, Integer> lineTotals =
        countLineTotalsForHours(
            List.of(currentHour), luasStopsById, nameToGtfsIds, luasToGtfs, allStopTimes);
    Map<String, TramLuasForecast> soonest = findSoonestPerStopDirection(forecasts);
    List<TramDelayDTO> delays = new ArrayList<>();
    for (TramLuasForecast forecast : soonest.values()) {
      TramDelayDTO dto =
          computeDelay(
              forecast,
              nowDublin,
              luasStopsById,
              nameToGtfsIds,
              luasToGtfs,
              hourlyPct,
              stopDirTrips,
              lineTotals);
      if (dto != null) delays.add(dto);
    }
    delays.sort(Comparator.comparingInt(TramDelayDTO::getDelayMins).reversed());
    return delays;
  }

  @Transactional(readOnly = true)
  public List<TramStopUsageDTO> getStopUsage(int startHour, int endHour) {
    log.info("Fetching stop usage for hours {}-{}", startHour, endHour);
    List<Integer> hours = expandHourRange(startHour, endHour);
    Map<String, TramStop> luasStopsById = buildLuasStopsMap();
    Map<String, List<String>> nameToGtfsIds = buildNameToGtfsIdsMap();
    Map<String, String> luasToGtfs = buildLuasToGtfsNameMap(luasStopsById, nameToGtfsIds);
    Map<String, Double> hourlyPctByLine = getHourlyPctForHours(hours);
    List<TramStopTime> allStopTimes = tramStopTimeRepository.findAll();
    Map<String, int[]> stopDirTrips = countTripsForHours(hours, nameToGtfsIds, allStopTimes);
    Map<String, Integer> lineTotals =
        countLineTotalsForHours(hours, luasStopsById, nameToGtfsIds, luasToGtfs, allStopTimes);

    List<TramStopUsageDTO> usageList = new ArrayList<>();
    for (TramStop luasStop : luasStopsById.values()) {
      String gtfsName =
          luasToGtfs.getOrDefault(
              luasStop.getStopId(), luasStop.getName().toLowerCase(Locale.ROOT));
      int[] dt = stopDirTrips.getOrDefault(gtfsName, new int[] {0, 0});
      int lineTotal = lineTotals.getOrDefault(luasStop.getLine(), 1);
      String lineKey = "-";
      double hourlyPct = hourlyPctByLine.getOrDefault(lineKey, 0.0) / 100.0;
      double dailyPax =
          "red".equals(luasStop.getLine()) ? RED_DAILY_PASSENGERS : GREEN_DAILY_PASSENGERS;
      long estIn = Math.round((double) dt[1] / Math.max(1, lineTotal) * hourlyPct * dailyPax);
      long estOut = Math.round((double) dt[0] / Math.max(1, lineTotal) * hourlyPct * dailyPax);
      usageList.add(
          TramStopUsageDTO.builder()
              .stopId(luasStop.getStopId())
              .stopName(luasStop.getName())
              .line(luasStop.getLine())
              .currentHour(startHour)
              .inboundTrips(dt[1])
              .outboundTrips(dt[0])
              .totalTrips(dt[0] + dt[1])
              .estimatedInboundPassengers(estIn)
              .estimatedOutboundPassengers(estOut)
              .estimatedTotalPassengers(estIn + estOut)
              .lat(luasStop.getLat())
              .lon(luasStop.getLon())
              .build());
    }
    usageList.sort(
        Comparator.comparingLong(TramStopUsageDTO::getEstimatedTotalPassengers).reversed());
    return usageList;
  }

  @Transactional(readOnly = true)
  public List<TramCommonDelayDTO> getCommonDelays() {
    Map<String, TramStop> luasStopsById = buildLuasStopsMap();
    List<Object[]> rows = tramDelayHistoryRepository.findAvgDelayPerStop();
    List<TramCommonDelayDTO> result = new ArrayList<>();
    for (Object[] row : rows) {
      String stopId = (String) row[0];
      TramStop stop = luasStopsById.get(stopId);
      result.add(
          TramCommonDelayDTO.builder()
              .stopId(stopId)
              .stopName((String) row[1])
              .line((String) row[2])
              .avgDelayMins(((Number) row[3]).doubleValue())
              .maxDelayMins(((Number) row[4]).intValue())
              .delayCount(((Number) row[5]).longValue())
              .lat(stop != null ? stop.getLat() : null)
              .lon(stop != null ? stop.getLon() : null)
              .build());
    }
    return result;
  }

  @Transactional(readOnly = true)
  public List<TramHourlyDistributionDTO> getHourlyDistribution() {
    String latestYear = tramHourlyDistributionRepository.findLatestYear();
    if (latestYear == null) return List.of();
    return tramHourlyDistributionRepository.findByYear(latestYear).stream()
        .map(this::mapToHourlyDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<TramStopDTO> getStops(int limit) {
    return tramStopRepository.findAll().stream()
        .limit(limit)
        .map(this::mapToStopDTO)
        .collect(Collectors.toList());
  }

  // ── Shared helpers ──────────────────────────────────────────────

  private Map<String, TramStop> buildLuasStopsMap() {
    return tramStopRepository.findAll().stream()
        .collect(Collectors.toMap(TramStop::getStopId, Function.identity()));
  }

  private Map<String, List<String>> buildNameToGtfsIdsMap() {
    Map<String, List<String>> map = new HashMap<>();
    for (TramGtfsStop gs : tramGtfsStopRepository.findAll()) {
      map.computeIfAbsent(gs.getName().toLowerCase(Locale.ROOT), k -> new ArrayList<>())
          .add(gs.getId());
    }
    return map;
  }

  private Map<String, String> buildLuasToGtfsNameMap(
      Map<String, TramStop> luasStopsById, Map<String, List<String>> nameToGtfsIds) {
    Map<String, String> mapping = new HashMap<>();
    // Also build a normalized lookup: strip " - " to handle "O'Connell - Upper" vs "O'Connell
    // Upper"
    Map<String, String> normalizedGtfs = new HashMap<>();
    for (String gtfsName : nameToGtfsIds.keySet()) {
      normalizedGtfs.put(gtfsName.replace(" - ", " ").replace("  ", " "), gtfsName);
    }
    for (TramStop luasStop : luasStopsById.values()) {
      String luasName = luasStop.getName().toLowerCase(Locale.ROOT);
      if (nameToGtfsIds.containsKey(luasName)) {
        mapping.put(luasStop.getStopId(), luasName);
      } else {
        // Try normalized match (strip dashes)
        String normalizedLuas = luasName.replace(" - ", " ").replace("  ", " ");
        String gtfsMatch = normalizedGtfs.get(normalizedLuas);
        if (gtfsMatch != null) {
          mapping.put(luasStop.getStopId(), gtfsMatch);
        } else {
          // Partial contains match
          for (String gtfsName : nameToGtfsIds.keySet()) {
            if (gtfsName.contains(luasName) || luasName.contains(gtfsName)) {
              mapping.put(luasStop.getStopId(), gtfsName);
              break;
            }
          }
        }
      }
    }
    return mapping;
  }

  private List<Integer> expandHourRange(int startHour, int endHour) {
    List<Integer> hours = new ArrayList<>();
    if (startHour <= endHour) {
      for (int h = startHour; h < endHour; h++) hours.add(h % 24);
    } else {
      for (int h = startHour; h < 25; h++) hours.add(h % 24);
      for (int h = 0; h < endHour; h++) hours.add(h);
    }
    if (hours.isEmpty()) hours.add(startHour % 24);
    return hours;
  }

  private Map<String, Double> getHourlyPctForHours(List<Integer> hours) {
    String latestYear = tramHourlyDistributionRepository.findLatestYear();
    if (latestYear == null) return Map.of();
    Set<Integer> hourSet = new HashSet<>(hours);
    Map<String, Double> result = new HashMap<>();
    tramHourlyDistributionRepository.findByYear(latestYear).stream()
        .filter(h -> hourSet.contains(parseHour(h.getTimeLabel())))
        .forEach(
            h ->
                result.merge(
                    h.getLineCode(), h.getValue() != null ? h.getValue() : 0.0, Double::sum));
    return result;
  }

  private Map<String, int[]> countTripsForHours(
      List<Integer> hours,
      Map<String, List<String>> nameToGtfsIds,
      List<TramStopTime> allStopTimes) {
    Map<String, String> gtfsIdToName = new HashMap<>();
    for (Map.Entry<String, List<String>> entry : nameToGtfsIds.entrySet()) {
      for (String gid : entry.getValue()) gtfsIdToName.put(gid, entry.getKey());
    }
    Set<Integer> hourSet = new HashSet<>(hours);
    Map<String, int[]> result = new HashMap<>();
    for (TramStopTime st : allStopTimes) {
      if (st.getArrivalTime() == null) continue;
      if (!hourSet.contains(st.getArrivalTime().toLocalTime().getHour())) continue;
      String stopName = gtfsIdToName.get(st.getStopId());
      if (stopName == null) continue;
      List<String> ids = nameToGtfsIds.get(stopName);
      int dirIdx = (ids != null && ids.size() > 1 && !st.getStopId().equals(ids.get(0))) ? 1 : 0;
      result.computeIfAbsent(stopName, k -> new int[] {0, 0})[dirIdx]++;
    }
    return result;
  }

  private Map<String, Integer> countLineTotalsForHours(
      List<Integer> hours,
      Map<String, TramStop> luasStopsById,
      Map<String, List<String>> nameToGtfsIds,
      Map<String, String> luasToGtfs,
      List<TramStopTime> allStopTimes) {
    Map<String, String> gtfsStopToLine = new HashMap<>();
    for (TramStop luasStop : luasStopsById.values()) {
      String gtfsName = luasToGtfs.get(luasStop.getStopId());
      List<String> ids = gtfsName != null ? nameToGtfsIds.get(gtfsName) : null;
      if (ids == null) {
        String nameKey = luasStop.getName().toLowerCase(Locale.ROOT);
        ids = nameToGtfsIds.get(nameKey);
        if (ids == null) ids = findGtfsStopIdsByPartialName(nameKey, nameToGtfsIds);
      }
      if (ids != null) {
        for (String gid : ids) gtfsStopToLine.put(gid, luasStop.getLine());
      }
    }
    Set<Integer> hourSet = new HashSet<>(hours);
    Map<String, Integer> totals = new HashMap<>();
    for (TramStopTime st : allStopTimes) {
      if (st.getArrivalTime() == null) continue;
      if (!hourSet.contains(st.getArrivalTime().toLocalTime().getHour())) continue;
      totals.merge(gtfsStopToLine.getOrDefault(st.getStopId(), "unknown"), 1, Integer::sum);
    }
    return totals;
  }

  private Map<String, TramLuasForecast> findSoonestPerStopDirection(
      List<TramLuasForecast> forecasts) {
    Map<String, TramLuasForecast> soonest = new HashMap<>();
    for (TramLuasForecast f : forecasts) {
      if (f.getDueMins() == null) continue;
      String key = f.getStopId() + "|" + f.getDirection();
      TramLuasForecast ex = soonest.get(key);
      if (ex == null || f.getDueMins() < ex.getDueMins()) soonest.put(key, f);
    }
    return soonest;
  }

  private TramDelayDTO computeDelay(
      TramLuasForecast forecast,
      LocalTime nowDublin,
      Map<String, TramStop> luasStopsById,
      Map<String, List<String>> nameToGtfsIds,
      Map<String, String> luasToGtfs,
      Map<String, Double> hourlyPct,
      Map<String, int[]> stopDirTrips,
      Map<String, Integer> lineTotals) {
    TramStop luasStop = luasStopsById.get(forecast.getStopId());
    if (luasStop == null) return null;
    String gtfsName = luasToGtfs.get(luasStop.getStopId());
    List<String> gtfsIds = gtfsName != null ? nameToGtfsIds.get(gtfsName) : null;
    if (gtfsIds == null || gtfsIds.isEmpty()) {
      String nk = luasStop.getName().toLowerCase(Locale.ROOT);
      gtfsIds = nameToGtfsIds.get(nk);
      if (gtfsIds == null || gtfsIds.isEmpty()) {
        gtfsIds = findGtfsStopIdsByPartialName(nk, nameToGtfsIds);
        if (gtfsIds.isEmpty()) return null;
      }
    }
    LocalTime predicted = nowDublin.plusMinutes(forecast.getDueMins());
    LocalTime nextSched = findNextScheduledArrival(gtfsIds, nowDublin);
    if (nextSched == null) return null;
    int delayMins = (int) java.time.Duration.between(nextSched, predicted).toMinutes();
    if (delayMins <= 0) return null;
    String lookupName = gtfsName != null ? gtfsName : luasStop.getName().toLowerCase(Locale.ROOT);
    int[] dt = stopDirTrips.getOrDefault(lookupName, new int[] {0, 0});
    int lineTotal = lineTotals.getOrDefault(forecast.getLine(), 1);
    double share = (double) (dt[0] + dt[1]) / Math.max(1, lineTotal);
    String lk = "-";
    double pct = hourlyPct.getOrDefault(lk, 0.0) / 100.0;
    double daily = "red".equals(forecast.getLine()) ? RED_DAILY_PASSENGERS : GREEN_DAILY_PASSENGERS;
    double affected = share * pct * daily * (delayMins / 60.0);
    return TramDelayDTO.builder()
        .stopId(forecast.getStopId())
        .stopName(luasStop.getName())
        .line(forecast.getLine())
        .direction(forecast.getDirection())
        .destination(forecast.getDestination())
        .scheduledTime(nextSched.toString())
        .dueMins(forecast.getDueMins())
        .delayMins(delayMins)
        .estimatedAffectedPassengers(Math.round(affected * 10.0) / 10.0)
        .build();
  }

  private LocalTime findNextScheduledArrival(List<String> gtfsStopIds, LocalTime now) {
    Time sqlNow = Time.valueOf(now);
    Time sqlEnd = Time.valueOf(now.plusMinutes(90));
    return tramStopTimeRepository
        .findByStopIdInAndArrivalTimeBetween(gtfsStopIds, sqlNow, sqlEnd)
        .stream()
        .map(st -> st.getArrivalTime().toLocalTime())
        .min(Comparator.naturalOrder())
        .orElse(null);
  }

  private List<String> findGtfsStopIdsByPartialName(
      String stopName, Map<String, List<String>> nameToGtfsIds) {
    for (Map.Entry<String, List<String>> entry : nameToGtfsIds.entrySet()) {
      if (entry.getKey().contains(stopName) || stopName.contains(entry.getKey())) {
        return entry.getValue();
      }
    }
    return List.of();
  }

  private TramLiveForecastDTO mapToLiveForecastDTO(
      TramLuasForecast f, Map<String, TramStop> stopsById) {
    TramStop s = stopsById.get(f.getStopId());
    return TramLiveForecastDTO.builder()
        .stopId(f.getStopId())
        .stopName(s != null ? s.getName() : f.getStopId())
        .line(f.getLine())
        .direction(f.getDirection())
        .destination(f.getDestination())
        .dueMins(f.getDueMins())
        .message(f.getMessage())
        .lat(s != null ? s.getLat() : null)
        .lon(s != null ? s.getLon() : null)
        .build();
  }

  private TramHourlyDistributionDTO mapToHourlyDTO(TramHourlyDistribution e) {
    return TramHourlyDistributionDTO.builder()
        .timeLabel(e.getTimeLabel())
        .line(e.getLineLabel())
        .percentage(e.getValue())
        .build();
  }

  private TramStopDTO mapToStopDTO(TramStop e) {
    TramStopDTO dto = new TramStopDTO();
    dto.setStopId(e.getStopId());
    dto.setLine(e.getLine());
    dto.setName(e.getName());
    dto.setLat(e.getLat());
    dto.setLon(e.getLon());
    dto.setParkRide(e.getParkRide());
    dto.setCycleRide(e.getCycleRide());
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
