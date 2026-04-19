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
      log.debug("Day plan MV not available, falling back to live query: {}", e.getMessage());
      return queryLive(date);
    }
  }

  @SuppressWarnings("unchecked")
  private List<Object[]> queryLive(LocalDate date) {
    try {
      return em.createNativeQuery(
              """
              SELECT e.id, e.event_name, e.venue_name, e.event_date, e.start_time, v.capacity,
                     'bus' AS transport_mode,
                     bs.id AS stop_id, bs.name AS stop_name, bs.lat, bs.lon,
                     STRING_AGG(DISTINCT br.short_name, ',' ORDER BY br.short_name) AS routes,
                     NULL::INT AS available_bikes,
                     MIN(earth_distance(ll_to_earth(COALESCE(e.latitude, v.latitude), COALESCE(e.longitude, v.longitude)),
                                        ll_to_earth(bs.lat, bs.lon)))::INT AS distance_m
              FROM external_data.events e
              JOIN external_data.venues v ON e.venue_id = v.id
              JOIN external_data.bus_stops bs
                ON earth_distance(ll_to_earth(COALESCE(e.latitude, v.latitude), COALESCE(e.longitude, v.longitude)),
                                  ll_to_earth(bs.lat, bs.lon)) <= 500
              JOIN external_data.bus_stop_times bst ON bst.stop_id = bs.id
              JOIN external_data.bus_trips bt ON bst.trip_id = bt.id
              JOIN external_data.bus_routes br ON bt.route_id = br.id
              WHERE e.event_date = :date
              GROUP BY e.id, e.event_name, e.venue_name, e.event_date, e.start_time, v.capacity,
                       bs.id, bs.name, bs.lat, bs.lon
              UNION ALL
              SELECT e.id, e.event_name, e.venue_name, e.event_date, e.start_time, v.capacity,
                     'tram',
                     ts.stop_id, ts.name, ts.lat, ts.lon, ts.line, NULL::INT,
                     earth_distance(ll_to_earth(COALESCE(e.latitude, v.latitude), COALESCE(e.longitude, v.longitude)),
                                    ll_to_earth(ts.lat, ts.lon))::INT
              FROM external_data.events e
              JOIN external_data.venues v ON e.venue_id = v.id
              JOIN external_data.tram_luas_stops ts
                ON earth_distance(ll_to_earth(COALESCE(e.latitude, v.latitude), COALESCE(e.longitude, v.longitude)),
                                  ll_to_earth(ts.lat, ts.lon)) <= 500
              WHERE e.event_date = :date
              UNION ALL
              SELECT e.id, e.event_name, e.venue_name, e.event_date, e.start_time, v.capacity,
                     'rail',
                     s.station_code, s.station_desc, s.lat, s.lon, NULL, NULL::INT,
                     earth_distance(ll_to_earth(COALESCE(e.latitude, v.latitude), COALESCE(e.longitude, v.longitude)),
                                    ll_to_earth(s.lat, s.lon))::INT
              FROM external_data.events e
              JOIN external_data.venues v ON e.venue_id = v.id
              JOIN external_data.irish_rail_stations s
                ON earth_distance(ll_to_earth(COALESCE(e.latitude, v.latitude), COALESCE(e.longitude, v.longitude)),
                                  ll_to_earth(s.lat, s.lon)) <= 500
              WHERE e.event_date = :date
              UNION ALL
              SELECT e.id, e.event_name, e.venue_name, e.event_date, e.start_time, v.capacity,
                     'bike',
                     b.station_id::TEXT, b.name, b.latitude::FLOAT, b.longitude::FLOAT,
                     NULL, b.capacity::INT,
                     earth_distance(ll_to_earth(COALESCE(e.latitude, v.latitude), COALESCE(e.longitude, v.longitude)),
                                    ll_to_earth(b.latitude::FLOAT, b.longitude::FLOAT))::INT
              FROM external_data.events e
              JOIN external_data.venues v ON e.venue_id = v.id
              JOIN external_data.dublin_bikes_stations b
                ON earth_distance(ll_to_earth(COALESCE(e.latitude, v.latitude), COALESCE(e.longitude, v.longitude)),
                                  ll_to_earth(b.latitude::FLOAT, b.longitude::FLOAT)) <= 500
              WHERE e.event_date = :date
              ORDER BY transport_mode, distance_m
              """)
          .setParameter("date", date)
          .getResultList();
    } catch (Exception e) {
      log.warn("Day plan live query failed: {}", e.getMessage());
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
