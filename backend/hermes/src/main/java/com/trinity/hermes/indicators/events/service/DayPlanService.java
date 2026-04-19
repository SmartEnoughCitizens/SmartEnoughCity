package com.trinity.hermes.indicators.events.service;

import com.trinity.hermes.indicators.events.dto.DayPlanDTO;
import com.trinity.hermes.indicators.events.dto.DayPlanEventRefDTO;
import com.trinity.hermes.indicators.events.dto.DayPlanModeDTO;
import com.trinity.hermes.indicators.events.dto.DayPlanStopDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Builds a transport-centric day plan by querying the {@code backend.mv_event_transport_proximity}
 * materialized view (registered and refreshed daily by {@link
 * com.trinity.hermes.mv.config.MvBootstrap}). Falls back to an empty plan if the MV has not yet
 * been populated.
 */
@Service
@Slf4j
public class DayPlanService {

  private static final List<String> MODE_ORDER = List.of("bus", "tram", "rail", "bike");

  @PersistenceContext private EntityManager em;

  public DayPlanDTO getDayPlan(LocalDate date) {
    List<Object[]> rows = queryMv(date);

    // mode → stopId → accumulator
    Map<String, Map<String, StopAccumulator>> byMode = new LinkedHashMap<>();
    for (String m : MODE_ORDER) byMode.put(m, new LinkedHashMap<>());

    for (Object[] r : rows) {
      // Columns: event_id, event_name, venue_name, event_date, start_time, capacity,
      //          transport_mode, stop_id, stop_name, lat, lon, routes, available_bikes, distance_m
      Integer eventId = r[0] instanceof Number n ? n.intValue() : null;
      String eventName = str(r[1]);
      String venueName = str(r[2]);
      String startTime = r[4] != null ? r[4].toString() : null;
      Integer capacity = r[5] instanceof Number n ? n.intValue() : null;
      String mode = str(r[6]);
      String stopId = str(r[7]);
      String stopName = str(r[8]);
      Double lat = r[9] instanceof Number n ? n.doubleValue() : null;
      Double lon = r[10] instanceof Number n ? n.doubleValue() : null;
      String routes = str(r[11]);
      Integer availableBikes = r[12] instanceof Number n ? n.intValue() : null;
      int distanceM = r[13] instanceof Number n ? n.intValue() : 0;

      if (mode == null || stopId == null || eventId == null) continue;

      Map<String, StopAccumulator> stops = byMode.computeIfAbsent(mode, k -> new LinkedHashMap<>());
      StopAccumulator acc =
          stops.computeIfAbsent(
              stopId, k -> new StopAccumulator(stopId, stopName, lat, lon, routes, availableBikes));

      String riskLevel = scoreRisk(capacity);
      acc.addEvent(
          new DayPlanEventRefDTO(eventId, eventName, venueName, startTime, riskLevel, distanceM));
    }

    List<DayPlanModeDTO> modes = new ArrayList<>();
    for (String mode : MODE_ORDER) {
      Map<String, StopAccumulator> stops = byMode.get(mode);
      if (stops == null || stops.isEmpty()) continue;
      List<DayPlanStopDTO> stopDtos =
          stops.values().stream()
              .map(StopAccumulator::toDto)
              .sorted((a, b) -> Integer.compare(minDist(a), minDist(b)))
              .collect(Collectors.toList());
      modes.add(new DayPlanModeDTO(mode, stopDtos));
    }

    return new DayPlanDTO(date.toString(), modes);
  }

  @SuppressWarnings("unchecked")
  private List<Object[]> queryMv(LocalDate date) {
    try {
      return em.createNativeQuery(
              """
              SELECT event_id, event_name, venue_name, event_date,
                     start_time, capacity, transport_mode,
                     stop_id, stop_name, lat, lon, routes, available_bikes, distance_m
              FROM backend.mv_event_transport_proximity
              WHERE event_date = :date
              ORDER BY transport_mode, distance_m
              """)
          .setParameter("date", date)
          .getResultList();
    } catch (Exception e) {
      log.warn("Day plan MV query failed (MV may not be populated yet): {}", e.getMessage());
      return List.of();
    }
  }

  private static int minDist(DayPlanStopDTO s) {
    return s.events().stream()
        .mapToInt(DayPlanEventRefDTO::distanceM)
        .min()
        .orElse(Integer.MAX_VALUE);
  }

  private static String scoreRisk(Integer capacity) {
    if (capacity == null) return "LOW";
    if (capacity >= 15_000) return "CRITICAL";
    if (capacity >= 5_000) return "HIGH";
    if (capacity >= 1_000) return "MEDIUM";
    return "LOW";
  }

  private static String str(Object o) {
    return o != null ? o.toString() : null;
  }

  private static class StopAccumulator {
    final String stopId;
    final String stopName;
    final Double lat;
    final Double lon;
    final List<String> routes;
    final Integer availableBikes;
    final List<DayPlanEventRefDTO> events = new ArrayList<>();

    StopAccumulator(
        String stopId,
        String stopName,
        Double lat,
        Double lon,
        String routesCsv,
        Integer availableBikes) {
      this.stopId = stopId;
      this.stopName = stopName;
      this.lat = lat;
      this.lon = lon;
      this.availableBikes = availableBikes;
      this.routes =
          routesCsv != null && !routesCsv.isBlank() ? List.of(routesCsv.split(",\\s*")) : List.of();
    }

    void addEvent(DayPlanEventRefDTO ref) {
      if (events.stream().noneMatch(e -> e.id().equals(ref.id()))) {
        events.add(ref);
      }
    }

    DayPlanStopDTO toDto() {
      return new DayPlanStopDTO(stopId, stopName, lat, lon, routes, availableBikes, events);
    }
  }
}
