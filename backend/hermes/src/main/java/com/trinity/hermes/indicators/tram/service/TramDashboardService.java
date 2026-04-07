package com.trinity.hermes.indicators.tram.service;

import com.trinity.hermes.disruptionmanagement.service.AlternativeTransportService;
import com.trinity.hermes.indicators.tram.dto.*;
import com.trinity.hermes.indicators.tram.entity.TramHourlyDistribution;
import com.trinity.hermes.indicators.tram.entity.TramLuasForecast;
import com.trinity.hermes.indicators.tram.entity.TramStop;
import com.trinity.hermes.indicators.tram.repository.*;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
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

  // Luas scheduled frequencies (minutes between trams)
  // Peak (7-9am, 4-7pm): every 3-5 min, Off-peak: every 7-10 min,
  // Late night / early morning: every 12-15 min
  private static final int PEAK_FREQUENCY_MINS = 5;
  private static final int OFFPEAK_FREQUENCY_MINS = 10;
  private static final int LATE_FREQUENCY_MINS = 15;

  // A tram is "delayed" if the soonest arrival exceeds the expected frequency
  // by this threshold. E.g. if expected every 10 min and soonest is 14 min, that's
  // a 4 min delay. We only report delays >= 2 min to filter noise.
  private static final int DELAY_REPORT_THRESHOLD_MINS = 2;

  private final TramStopRepository tramStopRepository;
  private final TramLuasForecastRepository tramLuasForecastRepository;
  private final TramHourlyDistributionRepository tramHourlyDistributionRepository;
  private final AlternativeTransportService alternativeTransportService;

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
    log.info("Fetching tram delays");

    List<TramLuasForecast> forecasts = tramLuasForecastRepository.findAllOrderedByLineAndStop();
    Map<String, TramStop> stopsById =
        tramStopRepository.findAll().stream()
            .collect(Collectors.toMap(TramStop::getStopId, Function.identity()));

    int currentHour = LocalTime.now(DUBLIN_ZONE).getHour();
    int expectedFrequency = getExpectedFrequency(currentHour);

    // Get hourly distribution for affected passenger estimates
    String latestYear = tramHourlyDistributionRepository.findLatestYear();
    Map<String, Double> hourlyPctByLine =
        latestYear != null
            ? tramHourlyDistributionRepository.findByYear(latestYear).stream()
                .filter(h -> parseHour(h.getTimeLabel()) == currentHour)
                .collect(
                    Collectors.toMap(
                        TramHourlyDistribution::getLineCode,
                        h -> h.getValue() != null ? h.getValue() : 0.0,
                        (a, b) -> a))
            : Map.of();

    // Group forecasts by stop+direction, find the SOONEST tram for each
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

    // Only report as delayed if soonest tram exceeds expected frequency + threshold
    return soonestPerStopDirection.values().stream()
        .filter(f -> f.getDueMins() > expectedFrequency + DELAY_REPORT_THRESHOLD_MINS)
        .map(f -> mapToDelayDTO(f, stopsById, hourlyPctByLine, expectedFrequency))
        .sorted(Comparator.comparingInt(TramDelayDTO::getDelayMins).reversed())
        .collect(Collectors.toList());
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

  // ── Stations list (for /api/v1/dashboard/tram) ──────────────────

  @Transactional(readOnly = true)
  public List<TramStopDTO> getStops(int limit) {
    log.debug("Fetching up to {} tram stops", limit);
    return tramStopRepository.findAll().stream()
        .limit(limit)
        .map(this::mapToStopDTO)
        .collect(Collectors.toList());
  }

  // ── Helpers ─────────────────────────────────────────────────────

  /**
   * Returns expected tram frequency in minutes based on time of day. Luas runs every ~4-5 min at
   * peak, ~7-10 min off-peak, ~12-15 min late night.
   */
  private int getExpectedFrequency(int hour) {
    if ((hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 19)) {
      return PEAK_FREQUENCY_MINS;
    } else if (hour >= 10 && hour <= 15) {
      return OFFPEAK_FREQUENCY_MINS;
    } else {
      return LATE_FREQUENCY_MINS;
    }
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

  private TramDelayDTO mapToDelayDTO(
      TramLuasForecast forecast,
      Map<String, TramStop> stopsById,
      Map<String, Double> hourlyPctByLine,
      int expectedFrequency) {
    TramStop stop = stopsById.get(forecast.getStopId());

    // Delay = how much longer than expected the soonest tram is
    int delayMins = forecast.getDueMins() - expectedFrequency;

    // Estimate affected passengers from hourly distribution
    String lineKey = "red".equals(forecast.getLine()) ? "-" : "--";
    Double hourlyPct = hourlyPctByLine.getOrDefault(lineKey, 0.0);
    // ~100k daily passengers across both lines; pro-rate by delay duration
    double estimatedAffected = (hourlyPct / 100.0) * 100_000 * (delayMins / 60.0);

    return TramDelayDTO.builder()
        .stopId(forecast.getStopId())
        .stopName(stop != null ? stop.getName() : forecast.getStopId())
        .line(forecast.getLine())
        .direction(forecast.getDirection())
        .destination(forecast.getDestination())
        .scheduledTime(LocalTime.now(DUBLIN_ZONE).minusMinutes(delayMins).toString())
        .dueMins(forecast.getDueMins())
        .delayMins(delayMins)
        .estimatedAffectedPassengers(Math.round(estimatedAffected * 10.0) / 10.0)
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
