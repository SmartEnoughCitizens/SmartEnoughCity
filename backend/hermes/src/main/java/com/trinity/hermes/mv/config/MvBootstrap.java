package com.trinity.hermes.mv.config;

import com.trinity.hermes.mv.dto.UpsertMvRequest;
import com.trinity.hermes.mv.repository.MvRegistryRepository;
import com.trinity.hermes.mv.service.MaterializedViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Registers application-owned materialized views on startup if they do not yet exist. Each MV is
 * created exactly once; subsequent restarts are no-ops. Use POST /api/v1/mv to update definitions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MvBootstrap {

  private final MaterializedViewService materializedViewService;
  private final MvRegistryRepository mvRegistryRepository;

  @EventListener(ApplicationReadyEvent.class)
  public void registerMvs() {
    registerIfAbsent(busStopWorstDelays());
    registerIfAbsent(eventTransportProximity());
    registerIfAbsent(trafficPeakSites());
    registerIfAbsent(latestTrainStationData());
  }

  private void registerIfAbsent(UpsertMvRequest request) {
    if (mvRegistryRepository.existsByName(request.getName())) {
      log.debug("MV '{}' already registered — skipping bootstrap", request.getName());
      return;
    }
    try {
      materializedViewService.upsert(request);
      log.info("Bootstrapped MV '{}'", request.getName());
    } catch (Exception e) {
      log.warn("Failed to bootstrap MV '{}': {}", request.getName(), e.getMessage());
    }
  }

  // ── MV definitions ────────────────────────────────────────────────────────

  /**
   * Pre-aggregates the worst arrival delay per (route, stop) from the last 15 minutes of live bus
   * data, restricted to the Dublin bounding box. Refreshed every 2 minutes so DisruptionDetection
   * reads a cached result instead of re-running the multi-join on every scan cycle.
   */
  private static UpsertMvRequest busStopWorstDelays() {
    String sql =
        """
        SELECT bt.route_id, bs.id AS stop_id, bs.name AS stop_name,
               bs.lat, bs.lon, MAX(stu.arrival_delay) AS max_delay
        FROM external_data.bus_live_trip_updates_stop_time_updates stu
        JOIN external_data.bus_live_trip_updates tu
             ON stu.trip_update_entry_id = tu.entry_id
        JOIN external_data.bus_trips bt ON tu.trip_id = bt.id
        JOIN external_data.bus_stops bs ON stu.stop_id = bs.id
        WHERE stu.arrival_delay > 1800
          AND tu.timestamp >= NOW() - INTERVAL '15 minutes'
          AND bs.lat BETWEEN 53.191981 AND 53.640914
          AND bs.lon BETWEEN -6.594926 AND -6.106449
        GROUP BY bt.route_id, bs.id, bs.name, bs.lat, bs.lon
        ORDER BY max_delay DESC
        """;

    UpsertMvRequest r = new UpsertMvRequest();
    r.setName("mv_bus_stop_worst_delays");
    r.setDescription(
        "Worst arrival delay per bus route+stop from live data, refreshed every 2 min");
    r.setViewSchema("backend");
    r.setQuerySql(sql.strip());
    r.setUniqueKeyColumns("route_id, stop_id");
    r.setRefreshCron("0 0/2 * * * *");
    r.setEnabled(true);
    return r;
  }

  /**
   * Pre-joins upcoming events (next 14 days) to all transport stops within 500 m, including
   * aggregated bus route names. Refreshed daily so the day-plan endpoint is a single indexed scan
   * rather than N per-event subqueries.
   */
  private static UpsertMvRequest eventTransportProximity() {
    String sql =
        """
        WITH bus_routes_by_stop AS (
            SELECT DISTINCT st.stop_id,
                   string_agg(r.short_name ORDER BY r.short_name) AS routes
            FROM external_data.bus_stop_times st
            JOIN external_data.bus_trips t ON t.id = st.trip_id
            JOIN external_data.bus_routes r ON r.id = t.route_id
            GROUP BY st.stop_id
        ),
        event_bus AS (
            SELECT e.id AS event_id, e.event_name, e.venue_name, e.event_date,
                   e.start_time, v.capacity,
                   'bus' AS transport_mode,
                   bs.id AS stop_id, bs.name AS stop_name,
                   bs.lat, bs.lon,
                   bsr.routes,
                   NULL::integer AS available_bikes,
                   public.EARTH_DISTANCE(
                       public.LL_TO_EARTH(e.latitude, e.longitude),
                       public.LL_TO_EARTH(bs.lat, bs.lon)
                   )::integer AS distance_m
            FROM external_data.events e
            LEFT JOIN external_data.venues v ON v.id = e.venue_id
            CROSS JOIN external_data.bus_stops bs
            LEFT JOIN bus_routes_by_stop bsr ON bsr.stop_id = bs.id
            WHERE e.event_date >= CURRENT_DATE
              AND e.event_date < CURRENT_DATE + INTERVAL '14 days'
              AND e.latitude IS NOT NULL AND e.longitude IS NOT NULL
              AND public.EARTH_DISTANCE(
                      public.LL_TO_EARTH(e.latitude, e.longitude),
                      public.LL_TO_EARTH(bs.lat, bs.lon)
                  ) <= 500
        ),
        event_tram AS (
            SELECT e.id AS event_id, e.event_name, e.venue_name, e.event_date,
                   e.start_time, v.capacity,
                   'tram' AS transport_mode,
                   ts.stop_id AS stop_id, ts.name AS stop_name,
                   ts.lat, ts.lon,
                   ts.line AS routes,
                   NULL::integer AS available_bikes,
                   public.EARTH_DISTANCE(
                       public.LL_TO_EARTH(e.latitude, e.longitude),
                       public.LL_TO_EARTH(ts.lat, ts.lon)
                   )::integer AS distance_m
            FROM external_data.events e
            LEFT JOIN external_data.venues v ON v.id = e.venue_id
            CROSS JOIN external_data.tram_luas_stops ts
            WHERE e.event_date >= CURRENT_DATE
              AND e.event_date < CURRENT_DATE + INTERVAL '14 days'
              AND e.latitude IS NOT NULL AND e.longitude IS NOT NULL
              AND public.EARTH_DISTANCE(
                      public.LL_TO_EARTH(e.latitude, e.longitude),
                      public.LL_TO_EARTH(ts.lat, ts.lon)
                  ) <= 500
        ),
        event_rail AS (
            SELECT e.id AS event_id, e.event_name, e.venue_name, e.event_date,
                   e.start_time, v.capacity,
                   'rail' AS transport_mode,
                   rs.station_code AS stop_id, rs.station_desc AS stop_name,
                   rs.lat, rs.lon,
                   NULL::text AS routes,
                   NULL::integer AS available_bikes,
                   public.EARTH_DISTANCE(
                       public.LL_TO_EARTH(e.latitude, e.longitude),
                       public.LL_TO_EARTH(rs.lat, rs.lon)
                   )::integer AS distance_m
            FROM external_data.events e
            LEFT JOIN external_data.venues v ON v.id = e.venue_id
            CROSS JOIN external_data.irish_rail_stations rs
            WHERE e.event_date >= CURRENT_DATE
              AND e.event_date < CURRENT_DATE + INTERVAL '14 days'
              AND e.latitude IS NOT NULL AND e.longitude IS NOT NULL
              AND public.EARTH_DISTANCE(
                      public.LL_TO_EARTH(e.latitude, e.longitude),
                      public.LL_TO_EARTH(rs.lat, rs.lon)
                  ) <= 500
        ),
        event_bike AS (
            SELECT e.id AS event_id, e.event_name, e.venue_name, e.event_date,
                   e.start_time, v.capacity,
                   'bike' AS transport_mode,
                   dbs.system_id AS stop_id, dbs.name AS stop_name,
                   dbs.latitude::double precision AS lat,
                   dbs.longitude::double precision AS lon,
                   NULL::text AS routes,
                   snap.available_bikes,
                   public.EARTH_DISTANCE(
                       public.LL_TO_EARTH(e.latitude, e.longitude),
                       public.LL_TO_EARTH(
                           dbs.latitude::double precision,
                           dbs.longitude::double precision)
                   )::integer AS distance_m
            FROM external_data.events e
            LEFT JOIN external_data.venues v ON v.id = e.venue_id
            CROSS JOIN external_data.dublin_bikes_stations dbs
            JOIN (
                SELECT DISTINCT ON (station_id) station_id, available_bikes
                FROM external_data.dublin_bikes_station_snapshots
                WHERE is_renting = true
                ORDER BY station_id, timestamp DESC
            ) snap ON snap.station_id = dbs.station_id
            WHERE e.event_date >= CURRENT_DATE
              AND e.event_date < CURRENT_DATE + INTERVAL '14 days'
              AND e.latitude IS NOT NULL AND e.longitude IS NOT NULL
              AND public.EARTH_DISTANCE(
                      public.LL_TO_EARTH(e.latitude, e.longitude),
                      public.LL_TO_EARTH(
                          dbs.latitude::double precision,
                          dbs.longitude::double precision)
                  ) <= 500
              AND snap.available_bikes > 0
        )
        SELECT * FROM event_bus
        UNION ALL SELECT * FROM event_tram
        UNION ALL SELECT * FROM event_rail
        UNION ALL SELECT * FROM event_bike
        """;

    UpsertMvRequest r = new UpsertMvRequest();
    r.setName("mv_event_transport_proximity");
    r.setDescription(
        "Events (next 14 days) joined to transport stops within 500 m, with bus routes."
            + " Refreshed daily for the day-plan export.");
    r.setViewSchema("backend");
    r.setQuerySql(sql.strip());
    r.setUniqueKeyColumns("event_id, transport_mode, stop_id");
    r.setRefreshCron("0 0 3 * * *");
    r.setEnabled(true);
    return r;
  }

  /**
   * Pre-aggregates the peak (MAX) avg_volume per traffic site, restricted to rows with valid
   * coordinates. Refreshed every 5 minutes so DisruptionDetection and CauseCorrelation read a
   * single indexed scan instead of a full GROUP BY on every cycle.
   */
  private static UpsertMvRequest trafficPeakSites() {
    String sql =
        """
        SELECT site_id, lat, lon, MAX(avg_volume) AS max_volume
        FROM backend.traffic_aggregated
        WHERE lat IS NOT NULL AND lon IS NOT NULL
        GROUP BY site_id, lat, lon
        ORDER BY max_volume DESC
        """;

    UpsertMvRequest r = new UpsertMvRequest();
    r.setName("mv_traffic_peak_sites");
    r.setDescription(
        "Peak avg_volume per traffic site, refreshed every 5 min for disruption detection");
    r.setViewSchema("backend");
    r.setQuerySql(sql.strip());
    r.setUniqueKeyColumns("site_id");
    r.setRefreshCron("0 0/5 * * * *");
    r.setEnabled(true);
    return r;
  }

  /**
   * Latest station-data record per (station_code, train_code), restricted to Dublin area stations.
   * Eliminates the expensive correlated subquery that DisruptionDetection ran on every 5-min cycle.
   * Refreshed every 5 minutes.
   */
  private static UpsertMvRequest latestTrainStationData() {
    String sql =
        """
        SELECT sd.id, sd.station_code, sd.train_code, sd.late_minutes
        FROM external_data.irish_rail_station_data sd
        JOIN external_data.irish_rail_stations s ON s.station_code = sd.station_code
        WHERE sd.fetched_at = (
            SELECT MAX(sd2.fetched_at)
            FROM external_data.irish_rail_station_data sd2
            WHERE sd2.station_code = sd.station_code
              AND sd2.train_code   = sd.train_code
        )
          AND s.lat BETWEEN 53.191981 AND 53.640914
          AND s.lon BETWEEN -6.594926 AND -6.106449
        ORDER BY sd.station_code, sd.late_minutes DESC
        """;

    UpsertMvRequest r = new UpsertMvRequest();
    r.setName("mv_latest_train_station_data");
    r.setDescription(
        "Latest station-data per (station_code, train_code) in Dublin, refreshed every 5 min");
    r.setViewSchema("backend");
    r.setQuerySql(sql.strip());
    r.setUniqueKeyColumns("id");
    r.setRefreshCron("0 0/5 * * * *");
    r.setEnabled(true);
    return r;
  }
}
