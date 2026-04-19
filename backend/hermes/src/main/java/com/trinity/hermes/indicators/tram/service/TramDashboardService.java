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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TramDashboardService {

  private static final ZoneId DUBLIN_ZONE = ZoneId.of("Europe/Dublin");

  // Fallback daily passenger counts — only used when no CSO data exists in DB.
  // Matches inference_engine/tram_utilisation.py _FALLBACK_DAILY_PASSENGERS.
  private static final double FALLBACK_RED_DAILY_PASSENGERS = 86_000.0;
  private static final double FALLBACK_GREEN_DAILY_PASSENGERS = 84_000.0;

  // ── Simulation constants ─────────────────────────────────────────
  //
  // Demand score = utilisation = estimated_passengers / (total_trips × tram_capacity)
  // Matches the recommendation engine (inference_engine/tram_utilisation.py).
  //
  // Simulation formula (capacity-based, same as recommendation engine):
  //   new_utilisation = old_utilisation × (current_trips / (current_trips + extra_trams))
  //
  // Adding extra trams increases capacity proportionally, spreading the same
  // passengers across more vehicles. No arbitrary constants needed.

  private final TramStopRepository tramStopRepository;
  private final TramLuasForecastRepository tramLuasForecastRepository;
  private final TramHourlyDistributionRepository tramHourlyDistributionRepository;
  private final TramStopTimeRepository tramStopTimeRepository;
  private final TramGtfsStopRepository tramGtfsStopRepository;
  private final TramDelayHistoryRepository tramDelayHistoryRepository;
  private final AlternativeTransportService alternativeTransportService;
  private final JdbcTemplate jdbcTemplate;

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
    Map<String, Double> dailyPassengers = loadDailyPassengers();
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
              lineTotals,
              dailyPassengers);
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
    // Usage counts rows (not unique trips) for both numerator and denominator —
    // the ratio is correct because both are inflated equally.
    Map<String, int[]> stopDirTrips = countStopTimeRows(hours, nameToGtfsIds, allStopTimes);
    Map<String, Integer> lineTotals =
        countLineStopTimeRows(hours, luasStopsById, nameToGtfsIds, luasToGtfs, allStopTimes);

    List<TramStopUsageDTO> usageList = new ArrayList<>();
    Map<String, Double> dailyPassengers = loadDailyPassengers();
    for (TramStop luasStop : luasStopsById.values()) {
      String gtfsName =
          luasToGtfs.getOrDefault(
              luasStop.getStopId(), luasStop.getName().toLowerCase(Locale.ROOT));
      int[] dt = stopDirTrips.getOrDefault(gtfsName, new int[] {0, 0});
      int lineTotal = lineTotals.getOrDefault(luasStop.getLine(), 1);
      // Usage uses combined hourly pct (key "-") as originally designed
      double hourlyPct = hourlyPctByLine.getOrDefault("-", 0.0) / 100.0;
      double dailyPax =
          dailyPassengers.getOrDefault(luasStop.getLine(), FALLBACK_RED_DAILY_PASSENGERS);
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

  // ── Demand & Simulation ──────────────────────────────────────────

  // Tram capacity per vehicle — must match inference_engine/tram_utilisation.py
  private static final Map<String, Integer> TRAM_CAPACITY = Map.of("red", 200, "green", 300);

  /**
   * Load daily passenger counts from CSO data using the same logic as the
   * inference engine's load_daily_passengers() in tram_utilisation.py.
   * Tries weekly data first, then monthly, then falls back to hardcoded values.
   */
  private Map<String, Double> loadDailyPassengers() {
    // 1. Try weekly CSO data (primary source — same query as Python)
    try {
      List<Map<String, Object>> weekly =
          jdbcTemplate.queryForList(
              "SELECT line_label, value FROM external_data.tram_passenger_journeys"
                  + " WHERE week_code = (SELECT MAX(week_code)"
                  + "   FROM external_data.tram_passenger_journeys)"
                  + " AND value IS NOT NULL");
      Map<String, Double> result = new HashMap<>();
      for (Map<String, Object> row : weekly) {
        String label = ((String) row.get("line_label")).trim().toLowerCase(Locale.ROOT);
        double value = ((Number) row.get("value")).doubleValue();
        if (label.contains("red")) result.put("red", value / 7.0);
        else if (label.contains("green")) result.put("green", value / 7.0);
      }
      if (!result.isEmpty()) {
        log.info("Daily passengers from CSO weekly: red={}, green={}", result.get("red"), result.get("green"));
        return result;
      }
    } catch (Exception e) {
      log.warn("Could not load weekly passenger data: {}", e.getMessage());
    }

    // 2. Fallback to monthly CSO data (same query as Python)
    try {
      List<Map<String, Object>> monthly =
          jdbcTemplate.queryForList(
              "SELECT statistic_label, value FROM external_data.tram_passenger_numbers"
                  + " WHERE year = (SELECT MAX(year) FROM external_data.tram_passenger_numbers)"
                  + " AND month_code = (SELECT MAX(month_code)"
                  + "   FROM external_data.tram_passenger_numbers"
                  + "   WHERE year = (SELECT MAX(year) FROM external_data.tram_passenger_numbers))"
                  + " AND value IS NOT NULL");
      Map<String, Double> result = new HashMap<>();
      for (Map<String, Object> row : monthly) {
        String label = ((String) row.get("statistic_label")).trim().toLowerCase(Locale.ROOT);
        double value = ((Number) row.get("value")).doubleValue();
        if (label.contains("red")) result.put("red", value / 30.0);
        else if (label.contains("green")) result.put("green", value / 30.0);
      }
      if (!result.isEmpty()) {
        log.info("Daily passengers from CSO monthly: red={}, green={}", result.get("red"), result.get("green"));
        return result;
      }
    } catch (Exception e) {
      log.warn("Could not load monthly passenger data: {}", e.getMessage());
    }

    // 3. Final fallback
    log.warn("No CSO passenger data found, using fallback values");
    Map<String, Double> fallback = new HashMap<>();
    fallback.put("red", FALLBACK_RED_DAILY_PASSENGERS);
    fallback.put("green", FALLBACK_GREEN_DAILY_PASSENGERS);
    return fallback;
  }

  @Transactional(readOnly = true)
  public List<TramStopDemandDTO> getStopDemand(int startHour, int endHour) {
    // Calculates utilisation the same way as the recommendation engine:
    //   utilisation = estimated_passengers / (total_trips × tram_capacity)
    //
    // Where estimated_passengers = (stop_trips / line_total_trips) × hourly_pct × daily_passengers
    // Uses the provided time period (e.g. Morning Peak 07:00–10:00).

    Map<String, TramStop> luasStops = buildLuasStopsMap();
    Map<String, List<String>> nameToGtfsIds = buildNameToGtfsIdsMap();
    Map<String, String> luasToGtfs = buildLuasToGtfsNameMap(luasStops, nameToGtfsIds);

    // Load stop times and hourly distribution for the requested time period
    // Uses weekday-only, real-service trips (>=10 stops) matching inference engine
    List<Integer> hours = expandHourRange(startHour, endHour);
    List<TramStopTime> realServiceStopTimes =
        tramStopTimeRepository.findWeekdayRealServiceStopTimes();
    log.info("Loaded {} weekday real-service stop time records", realServiceStopTimes.size());
    Map<String, int[]> stopDirTrips =
        countTripsForHours(hours, nameToGtfsIds, realServiceStopTimes);
    Map<String, Integer> lineTotals =
        countLineTotalsForHours(hours, luasStops, nameToGtfsIds, luasToGtfs, realServiceStopTimes);
    Map<String, Double> hourlyPctByLine = getHourlyPctForHours(hours);

    log.info("Demand calc: lineTotals={}, hourlyPct={}", lineTotals, hourlyPctByLine);

    Map<String, Double> dailyPassengers = loadDailyPassengers();
    List<TramStopDemandDTO> results = new ArrayList<>();
    boolean logged = false;
    for (TramStop stop : luasStops.values()) {
      String gtfsName =
          luasToGtfs.getOrDefault(stop.getStopId(), stop.getName().toLowerCase(Locale.ROOT));
      int[] dt = stopDirTrips.getOrDefault(gtfsName, new int[] {0, 0});
      int totalTrips = dt[0] + dt[1];
      int lineTotal = lineTotals.getOrDefault(stop.getLine(), 1);

      // Estimate passengers: same formula as inference engine
      // hourlyPctByLine is keyed by "red" / "green" matching the recommendation engine
      double hourlyPct = hourlyPctByLine.getOrDefault(stop.getLine(), 0.0) / 100.0;
      double dailyPax =
          dailyPassengers.getOrDefault(stop.getLine(), FALLBACK_RED_DAILY_PASSENGERS);
      double estIn = (double) dt[1] / Math.max(1, lineTotal) * hourlyPct * dailyPax;
      double estOut = (double) dt[0] / Math.max(1, lineTotal) * hourlyPct * dailyPax;
      double estTotal = estIn + estOut;

      // Utilisation = estimated_passengers / capacity
      int tramCap = TRAM_CAPACITY.getOrDefault(stop.getLine(), 200);
      double capacity = totalTrips * tramCap;
      double utilisation = capacity > 0 ? Math.min(1.0, estTotal / capacity) : 0.0;

      if (!logged && "red".equals(stop.getLine()) && totalTrips > 0) {
        log.info(
            "Sample stop [{}]: dt=[{},{}] totalTrips={} lineTotal={} hourlyPct={} dailyPax={} estTotal={} capacity={} util={}",
            stop.getName(),
            dt[0],
            dt[1],
            totalTrips,
            lineTotal,
            hourlyPct,
            dailyPax,
            estTotal,
            capacity,
            utilisation);
        logged = true;
      }

      results.add(
          TramStopDemandDTO.builder()
              .stopId(stop.getStopId())
              .stopName(stop.getName())
              .line(stop.getLine())
              .lat(stop.getLat())
              .lon(stop.getLon())
              .tripCount(totalTrips)
              .demandScore(Math.round(utilisation * 1_000_000.0) / 1_000_000.0)
              .build());
    }
    return results;
  }

  @Transactional(readOnly = true)
  public TramDemandSimulateResponseDTO simulateDemand(TramDemandSimulateRequestDTO request) {
    String targetLine = request.getLine() != null ? request.getLine().toLowerCase(Locale.ROOT) : "";
    // Allow -20 to +20: positive = add trams, negative = remove trams
    int extraTrams = Math.max(-20, Math.min(20, request.getExtraTrams()));
    String originStopId = request.getOriginStopId();
    String destinationStopId = request.getDestinationStopId();
    int startHour = request.getStartHour() > 0 ? request.getStartHour() : 7;
    int endHour = request.getEndHour() > 0 ? request.getEndHour() : 10;

    List<TramStopDemandDTO> baseDemand = getStopDemand(startHour, endHour);

    // Build the set of affected stop IDs: if origin and destination are provided,
    // only affect stops on the target line that fall between origin and destination
    // based on their geographic position along the line (ordered by lat for green,
    // lon for red which runs more east-west).
    Set<String> corridorStopIds = null;
    if (originStopId != null
        && destinationStopId != null
        && !originStopId.isBlank()
        && !destinationStopId.isBlank()) {
      corridorStopIds =
          computeCorridorStops(baseDemand, targetLine, originStopId, destinationStopId);
    }

    List<TramStopDemandDTO> simulated = new ArrayList<>();
    List<String> affectedStopIds = new ArrayList<>();

    for (TramStopDemandDTO stop : baseDemand) {
      boolean onTargetLine =
          stop.getLine() != null
              && stop.getLine().toLowerCase(Locale.ROOT).equals(targetLine)
              && stop.getTripCount() > 0;

      boolean isAffected =
          onTargetLine && (corridorStopIds == null || corridorStopIds.contains(stop.getStopId()));

      if (!isAffected) {
        simulated.add(stop);
        continue;
      }

      // Capacity-based relief — same logic as the recommendation engine:
      //   utilisation = passengers / (trams × tram_capacity)
      //   Adding trams (+) increases capacity → utilisation drops
      //   Removing trams (-) decreases capacity → utilisation rises
      //   new_utilisation = old_utilisation × (current_trams / (current_trams + extra_trams))
      //
      // Examples with 50 current trams at 78% utilisation:
      //   Add 3:    78% × (50/53) = 73.6%  → less crowded
      //   Remove 3: 78% × (50/47) = 83.0%  → more crowded
      int currentTrips = stop.getTripCount();
      int newTrips = Math.max(1, currentTrips + extraTrams); // never go below 1 tram
      double newScore = stop.getDemandScore() * ((double) currentTrips / newTrips);
      newScore = Math.min(1.0, Math.max(0.0, newScore)); // clamp 0–100%

      simulated.add(
          TramStopDemandDTO.builder()
              .stopId(stop.getStopId())
              .stopName(stop.getStopName())
              .line(stop.getLine())
              .lat(stop.getLat())
              .lon(stop.getLon())
              .tripCount(newTrips)
              .demandScore(Math.round(newScore * 1_000_000.0) / 1_000_000.0)
              .build());
      affectedStopIds.add(stop.getStopId());
    }

    return TramDemandSimulateResponseDTO.builder()
        .baseDemand(baseDemand)
        .simulatedDemand(simulated)
        .affectedStopIds(affectedStopIds)
        .build();
  }

  /**
   * Given a line and two endpoint stop IDs, determine all stops on that line that lie between the
   * two endpoints. Uses geographic ordering along the line.
   */
  private Set<String> computeCorridorStops(
      List<TramStopDemandDTO> allDemand,
      String line,
      String originStopId,
      String destinationStopId) {

    // Get all stops on the target line with valid coordinates
    List<TramStopDemandDTO> lineStops =
        allDemand.stream()
            .filter(
                s ->
                    s.getLine() != null
                        && s.getLine().toLowerCase(Locale.ROOT).equals(line)
                        && s.getLat() != null
                        && s.getLon() != null)
            .collect(Collectors.toList());

    // Sort stops geographically along the line.
    // Green line runs roughly north-south → sort by latitude (descending = north to south).
    // Red line runs roughly east-west → sort by longitude.
    if ("green".equals(line)) {
      lineStops.sort(Comparator.comparingDouble(TramStopDemandDTO::getLat).reversed());
    } else {
      lineStops.sort(Comparator.comparingDouble(TramStopDemandDTO::getLon));
    }

    // Find indices of origin and destination in the sorted list
    int originIdx = -1;
    int destIdx = -1;
    for (int i = 0; i < lineStops.size(); i++) {
      String sid = lineStops.get(i).getStopId();
      if (sid.equals(originStopId)) originIdx = i;
      if (sid.equals(destinationStopId)) destIdx = i;
    }

    // If either endpoint not found, fall back to affecting all stops on the line
    if (originIdx == -1 || destIdx == -1) {
      return lineStops.stream().map(TramStopDemandDTO::getStopId).collect(Collectors.toSet());
    }

    int fromIdx = Math.min(originIdx, destIdx);
    int toIdx = Math.max(originIdx, destIdx);

    Set<String> corridor = new HashSet<>();
    for (int i = fromIdx; i <= toIdx; i++) {
      corridor.add(lineStops.get(i).getStopId());
    }
    return corridor;
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
            h -> {
              // Map to per-line keys ("red", "green") matching the inference engine,
              // using line_label which contains "Red Line", "Green Line", etc.
              String lk = h.getLineLabel().trim().toLowerCase(Locale.ROOT);
              String key;
              if (lk.contains("red")) {
                key = "red";
              } else if (lk.contains("green")) {
                key = "green";
              } else {
                key = h.getLineCode(); // fallback to line_code (e.g. "-" for combined)
              }
              result.merge(key, h.getValue() != null ? h.getValue() : 0.0, Double::sum);
            });
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
    // Count UNIQUE trips per stop per direction (matching inference engine's trip_id.nunique())
    Map<String, Set<String>[]> tripSets = new HashMap<>();
    for (TramStopTime st : allStopTimes) {
      if (st.getArrivalTime() == null) continue;
      if (!hourSet.contains(st.getArrivalTime().toLocalTime().getHour())) continue;
      String stopName = gtfsIdToName.get(st.getStopId());
      if (stopName == null) continue;
      List<String> ids = nameToGtfsIds.get(stopName);
      int dirIdx = (ids != null && ids.size() > 1 && !st.getStopId().equals(ids.get(0))) ? 1 : 0;
      @SuppressWarnings("unchecked")
      Set<String>[] sets =
          tripSets.computeIfAbsent(stopName, k -> new Set[] {new HashSet<>(), new HashSet<>()});
      sets[dirIdx].add(st.getTripId());
    }
    Map<String, int[]> result = new HashMap<>();
    for (Map.Entry<String, Set<String>[]> e : tripSets.entrySet()) {
      result.put(e.getKey(), new int[] {e.getValue()[0].size(), e.getValue()[1].size()});
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
    // Count UNIQUE trips per line (matching inference engine's
    // groupby("line")["trip_id"].nunique())
    Map<String, Set<String>> lineTrips = new HashMap<>();
    for (TramStopTime st : allStopTimes) {
      if (st.getArrivalTime() == null) continue;
      if (!hourSet.contains(st.getArrivalTime().toLocalTime().getHour())) continue;
      String line = gtfsStopToLine.getOrDefault(st.getStopId(), "unknown");
      lineTrips.computeIfAbsent(line, k -> new HashSet<>()).add(st.getTripId());
    }
    Map<String, Integer> totals = new HashMap<>();
    for (Map.Entry<String, Set<String>> e : lineTrips.entrySet()) {
      totals.put(e.getKey(), e.getValue().size());
    }
    return totals;
  }

  // ── Row-counting helpers (used by getStopUsage — original logic) ──────

  /** Count stop_time rows per stop per direction (NOT unique trips). Used by usage tab. */
  private Map<String, int[]> countStopTimeRows(
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

  /** Count stop_time rows per line (NOT unique trips). Used by usage tab. */
  private Map<String, Integer> countLineStopTimeRows(
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
      Map<String, Integer> lineTotals,
      Map<String, Double> dailyPassengers) {
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
    double daily = dailyPassengers.getOrDefault(forecast.getLine(), FALLBACK_RED_DAILY_PASSENGERS);
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
