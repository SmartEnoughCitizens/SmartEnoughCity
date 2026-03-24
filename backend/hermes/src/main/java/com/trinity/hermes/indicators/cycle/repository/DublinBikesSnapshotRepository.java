package com.trinity.hermes.indicators.cycle.repository;

import com.trinity.hermes.indicators.cycle.entity.DublinBikesSnapshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DublinBikesSnapshotRepository extends JpaRepository<DublinBikesSnapshot, Integer> {

  /**
   * Latest snapshot per station joined with station metadata. Returns Object[] rows with columns:
   * station_id, name, short_name, address, latitude, longitude, capacity, region_id,
   * available_bikes, available_docks, disabled_bikes, disabled_docks, is_installed, is_renting,
   * is_returning, last_reported, snapshot_timestamp
   */
  @Query(
      value =
          """
          SELECT DISTINCT ON (s.station_id)
              st.station_id,
              st.name,
              st.short_name,
              st.address,
              st.latitude,
              st.longitude,
              st.capacity,
              st.region_id,
              s.available_bikes,
              s.available_docks,
              s.disabled_bikes,
              s.disabled_docks,
              s.is_installed,
              s.is_renting,
              s.is_returning,
              s.last_reported,
              s.timestamp AS snapshot_timestamp
          FROM external_data.dublin_bikes_station_snapshots s
          JOIN external_data.dublin_bikes_stations st ON s.station_id = st.station_id
          WHERE s.timestamp >= NOW() - INTERVAL '10 minutes'
          ORDER BY s.station_id, s.timestamp DESC
          """,
      nativeQuery = true)
  List<Object[]> findLatestSnapshotPerStation();

  /**
   * Aggregated network summary from latest snapshot per station. Returns a single Object[] row
   * with: total_stations, total_bikes, total_docks, disabled_bikes, disabled_docks, empty_stations,
   * full_stations, avg_fullness_pct, latest_timestamp
   */
  @Query(
      value =
          """
          WITH latest AS (
              SELECT DISTINCT ON (s.station_id)
                  s.station_id, s.available_bikes, s.available_docks,
                  s.disabled_bikes, s.disabled_docks, s.is_installed, s.timestamp
              FROM external_data.dublin_bikes_station_snapshots s
              WHERE s.timestamp >= NOW() - INTERVAL '10 minutes'
              ORDER BY s.station_id, s.timestamp DESC
          )
          SELECT
              (SELECT COUNT(*) FROM external_data.dublin_bikes_stations)                   AS total_stations,
              COUNT(*)                                                                     AS active_stations,
              COALESCE(SUM(l.available_bikes), 0)                                          AS total_bikes,
              COALESCE(SUM(l.available_docks), 0)                                          AS total_docks,
              COALESCE(SUM(l.disabled_bikes), 0)                                           AS disabled_bikes,
              COALESCE(SUM(l.disabled_docks), 0)                                           AS disabled_docks,
              COUNT(CASE WHEN l.available_bikes = 0 AND l.is_installed THEN 1 END)         AS empty_stations,
              COUNT(CASE WHEN l.available_docks = 0 AND l.is_installed THEN 1 END)         AS full_stations,
              COALESCE(AVG((st.capacity - l.available_docks)::float / NULLIF(st.capacity, 0) * 100), 0) AS avg_fullness_pct,
              MAX(l.timestamp)                                                              AS latest_timestamp
          FROM latest l
          JOIN external_data.dublin_bikes_stations st ON l.station_id = st.station_id
          WHERE l.is_installed = true
          """,
      nativeQuery = true)
  List<Object[]> findNetworkSummary();

  /**
   * Region-level aggregations from latest snapshots. Returns Object[] rows with: region_id,
   * station_count, total_capacity, avg_usage_rate, avg_available_bikes, avg_available_docks,
   * empty_stations, full_stations
   */
  @Query(
      value =
          """
          WITH latest AS (
              SELECT DISTINCT ON (s.station_id)
                  s.station_id, s.available_bikes, s.available_docks, s.is_installed
              FROM external_data.dublin_bikes_station_snapshots s
              WHERE s.timestamp >= NOW() - INTERVAL '10 minutes'
              ORDER BY s.station_id, s.timestamp DESC
          )
          SELECT
              st.region_id,
              COUNT(*)                                                          AS station_count,
              SUM(st.capacity)                                                  AS total_capacity,
              AVG((st.capacity - l.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS avg_usage_rate,
              AVG(l.available_bikes)                                            AS avg_available_bikes,
              AVG(l.available_docks)                                            AS avg_available_docks,
              COUNT(CASE WHEN l.available_bikes = 0 THEN 1 END)               AS empty_stations,
              COUNT(CASE WHEN l.available_docks = 0 THEN 1 END)               AS full_stations
          FROM latest l
          JOIN external_data.dublin_bikes_stations st ON l.station_id = st.station_id
          WHERE l.is_installed = true
          GROUP BY st.region_id
          ORDER BY avg_usage_rate DESC
          """,
      nativeQuery = true)
  List<Object[]> findRegionMetrics();

  /**
   * Network imbalance score: standard deviation of fullness % from 50% across all active stations
   * (latest snapshot). Returns a single Object[] with: imbalance_score
   */
  @Query(
      value =
          """
          WITH latest AS (
              SELECT DISTINCT ON (s.station_id)
                  s.station_id, s.available_docks
              FROM external_data.dublin_bikes_station_snapshots s
              WHERE s.timestamp >= NOW() - INTERVAL '10 minutes'
              ORDER BY s.station_id, s.timestamp DESC
          ),
          fullness AS (
              SELECT ABS((st.capacity - l.available_docks)::float / NULLIF(st.capacity, 0) * 100 - 50) AS deviation
              FROM latest l
              JOIN external_data.dublin_bikes_stations st ON l.station_id = st.station_id
              WHERE st.capacity > 0
          )
          SELECT AVG(deviation) AS imbalance_score FROM fullness
          """,
      nativeQuery = true)
  List<Object[]> findNetworkImbalanceScore();

  // -------------------------------------------------------------------------
  // Station rankings (today's snapshots)
  // -------------------------------------------------------------------------

  /**
   * Today's station rankings ordered by avg usage rate descending (busiest first). Window: midnight
   * UTC today → now. Returns Object[] rows: station_id, name, avg_usage_rate
   */
  @Query(
      value =
          """
          SELECT
              s.station_id,
              st.name,
              AVG((st.capacity - s.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS avg_usage_rate
          FROM external_data.dublin_bikes_station_snapshots s
          JOIN external_data.dublin_bikes_stations st ON s.station_id = st.station_id
          WHERE s.timestamp >= DATE_TRUNC('day', NOW() AT TIME ZONE 'UTC')
          GROUP BY s.station_id, st.name
          ORDER BY avg_usage_rate DESC
          LIMIT :limitVal
          """,
      nativeQuery = true)
  List<Object[]> findBusiestStations(@Param("limitVal") int limit);

  /**
   * Today's station rankings ordered by avg usage rate ascending (least used first). Window:
   * midnight UTC today → now. Returns Object[] rows: station_id, name, avg_usage_rate
   */
  @Query(
      value =
          """
          SELECT
              s.station_id,
              st.name,
              AVG((st.capacity - s.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS avg_usage_rate
          FROM external_data.dublin_bikes_station_snapshots s
          JOIN external_data.dublin_bikes_stations st ON s.station_id = st.station_id
          WHERE s.timestamp >= DATE_TRUNC('day', NOW() AT TIME ZONE 'UTC')
          GROUP BY s.station_id, st.name
          ORDER BY avg_usage_rate ASC
          LIMIT :limitVal
          """,
      nativeQuery = true)
  List<Object[]> findLeastUsedStations(@Param("limitVal") int limit);

  // -------------------------------------------------------------------------
  // Rebalancing suggestions
  // -------------------------------------------------------------------------

  /**
   * Pairs each empty station (available_bikes = 0) with its nearest surplus station
   * (available_bikes > 0) from the latest 10-minute snapshot window. One row per empty station.
   * Returns Object[] rows: source_station_id, source_name, source_lat, source_lon, source_bikes,
   * target_station_id, target_name, target_lat, target_lon, target_capacity, distance_km
   */
  @Query(
      value =
          """
          WITH latest AS (
              SELECT DISTINCT ON (s.station_id)
                  s.station_id, s.available_bikes, s.available_docks, s.is_installed
              FROM external_data.dublin_bikes_station_snapshots s
              WHERE s.timestamp >= NOW() - INTERVAL '10 minutes'
              ORDER BY s.station_id, s.timestamp DESC
          ),
          surplus_stations AS (
              SELECT l.station_id, l.available_bikes, l.available_docks,
                     st.name, st.latitude, st.longitude
              FROM latest l
              JOIN external_data.dublin_bikes_stations st ON l.station_id = st.station_id
              WHERE l.available_bikes > 10 AND l.is_installed = true
          ),
          empty_stations AS (
              SELECT l.station_id, l.available_bikes, l.available_docks,
                     st.name, st.latitude, st.longitude, st.capacity
              FROM latest l
              JOIN external_data.dublin_bikes_stations st ON l.station_id = st.station_id
              WHERE l.available_bikes = 0 AND l.is_installed = true
          )
          SELECT DISTINCT ON (e.station_id)
              s.station_id                                                           AS source_station_id,
              s.name                                                                 AS source_name,
              s.latitude                                                             AS source_lat,
              s.longitude                                                            AS source_lon,
              s.available_bikes                                                      AS source_bikes,
              e.station_id                                                           AS target_station_id,
              e.name                                                                 AS target_name,
              e.latitude                                                             AS target_lat,
              e.longitude                                                            AS target_lon,
              e.capacity                                                             AS target_capacity,
              SQRT(POWER((s.latitude::float - e.latitude::float), 2)
                 + POWER((s.longitude::float - e.longitude::float)
                     * COS(RADIANS((s.latitude::float + e.latitude::float) / 2.0)), 2)
                 ) * 111.0                                                           AS distance_km
          FROM empty_stations e
          CROSS JOIN surplus_stations s
          WHERE e.station_id != s.station_id
          ORDER BY e.station_id, distance_km ASC
          LIMIT :limitVal
          """,
      nativeQuery = true)
  List<Object[]> findRebalancingSuggestions(@Param("limitVal") int limit);

  // -------------------------------------------------------------------------
  // Demand Analysis (snapshot-based hourly aggregations)
  // -------------------------------------------------------------------------

  /**
   * Network-wide average usage rate grouped by hour-of-day (Europe/Dublin timezone). Covers the
   * last {@code days} days. Returns Object[] rows: hour_of_day (int), avg_usage_rate (double),
   * station_count (long)
   */
  @Query(
      value =
          """
          SELECT
              EXTRACT(HOUR FROM s.timestamp AT TIME ZONE 'Europe/Dublin')::int   AS hour_of_day,
              AVG((st.capacity - s.available_docks)::float
                  / NULLIF(st.capacity, 0) * 100)                                AS avg_usage_rate,
              COUNT(DISTINCT s.station_id)                                       AS station_count
          FROM external_data.dublin_bikes_station_snapshots s
          JOIN external_data.dublin_bikes_stations st ON s.station_id = st.station_id
          WHERE s.timestamp >= NOW() - make_interval(days => :days)
            AND st.capacity > 0
            AND s.is_installed = true
          GROUP BY hour_of_day
          ORDER BY hour_of_day ASC
          """,
      nativeQuery = true)
  List<Object[]> findNetworkHourlyProfile(@Param("days") int days);

  /**
   * Per-station peak-hour classification based on the last {@code days} days of snapshots. Returns
   * Object[] rows: station_id (int), name (String), peak_hour (int), peak_usage (double),
   * classification (String)
   */
  @Query(
      value =
          """
          WITH hourly AS (
              SELECT
                  s.station_id,
                  st.name,
                  EXTRACT(HOUR FROM s.timestamp AT TIME ZONE 'Europe/Dublin')::int  AS hour_of_day,
                  AVG((st.capacity - s.available_docks)::float
                      / NULLIF(st.capacity, 0) * 100)                               AS avg_usage
              FROM external_data.dublin_bikes_station_snapshots s
              JOIN external_data.dublin_bikes_stations st ON s.station_id = st.station_id
              WHERE s.timestamp >= NOW() - make_interval(days => :days)
                AND st.capacity > 0
                AND s.is_installed = true
              GROUP BY s.station_id, st.name, hour_of_day
          ),
          peak AS (
              SELECT DISTINCT ON (station_id)
                  station_id,
                  name,
                  hour_of_day  AS peak_hour,
                  avg_usage    AS peak_usage
              FROM hourly
              ORDER BY station_id, avg_usage DESC
          )
          SELECT
              p.station_id,
              p.name,
              p.peak_hour,
              p.peak_usage,
              CASE
                  WHEN p.peak_hour BETWEEN 7  AND 9  THEN 'MORNING_PEAK'
                  WHEN p.peak_hour BETWEEN 12 AND 14 THEN 'AFTERNOON_PEAK'
                  WHEN p.peak_hour BETWEEN 17 AND 19 THEN 'EVENING_PEAK'
                  ELSE 'OFF_PEAK'
              END AS classification
          FROM peak p
          ORDER BY p.peak_usage DESC
          """,
      nativeQuery = true)
  List<Object[]> findStationClassification(@Param("days") int days);

  /**
   * Per-station, per-hour average usage rate for the top {@code stationLimit} busiest stations.
   * Returns Object[] rows: station_id (int), name (String), hour_of_day (int), avg_usage_rate
   * (double)
   */
  @Query(
      value =
          """
          WITH top_stations AS (
              SELECT s.station_id
              FROM external_data.dublin_bikes_station_snapshots s
              JOIN external_data.dublin_bikes_stations st ON s.station_id = st.station_id
              WHERE s.timestamp >= NOW() - make_interval(days => :days)
                AND st.capacity > 0
                AND s.is_installed = true
              GROUP BY s.station_id
              ORDER BY AVG((st.capacity - s.available_docks)::float / NULLIF(st.capacity, 0) * 100) DESC
              LIMIT :stationLimit
          )
          SELECT
              s.station_id,
              st.name,
              EXTRACT(HOUR FROM s.timestamp AT TIME ZONE 'Europe/Dublin')::int  AS hour_of_day,
              AVG((st.capacity - s.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS avg_usage_rate
          FROM external_data.dublin_bikes_station_snapshots s
          JOIN external_data.dublin_bikes_stations st ON s.station_id = st.station_id
          JOIN top_stations ts ON s.station_id = ts.station_id
          WHERE s.timestamp >= NOW() - make_interval(days => :days)
            AND st.capacity > 0
            AND s.is_installed = true
          GROUP BY s.station_id, st.name, hour_of_day
          ORDER BY s.station_id, hour_of_day
          """,
      nativeQuery = true)
  List<Object[]> findStationHourlyUsage(
      @Param("days") int days, @Param("stationLimit") int stationLimit);

  /**
   * Estimates origin–destination trip flows using a gravity model over snapshot availability
   * changes. Departure events (available_bikes decreasing) at one station are distributed to nearby
   * arrival events (available_bikes increasing) proportional to arrival volume. Returns Object[]
   * rows: origin_station_id, origin_name, origin_lat, origin_lon, dest_station_id, dest_name,
   * dest_lat, dest_lon, estimated_trips, distance_km
   */
  @Query(
      value =
          """
          WITH snapshot_deltas AS (
              SELECT
                  station_id,
                  available_bikes
                      - LAG(available_bikes) OVER (PARTITION BY station_id ORDER BY timestamp) AS delta
              FROM external_data.dublin_bikes_station_snapshots
              WHERE timestamp >= NOW() - make_interval(days => :days)
                AND is_installed = true
          ),
          station_flows AS (
              SELECT
                  station_id,
                  SUM(CASE WHEN delta < 0 AND delta >= -10 THEN -delta ELSE 0 END)::float AS departures,
                  SUM(CASE WHEN delta > 0 AND delta <= 10  THEN  delta ELSE 0 END)::float AS arrivals
              FROM snapshot_deltas
              WHERE delta IS NOT NULL
              GROUP BY station_id
              HAVING SUM(CASE WHEN delta < 0 AND delta >= -10 THEN -delta ELSE 0 END) > 0
                  OR SUM(CASE WHEN delta > 0 AND delta <= 10  THEN  delta ELSE 0 END) > 0
          ),
          station_info AS (
              SELECT sf.station_id, sf.departures, sf.arrivals,
                     st.name, st.latitude, st.longitude
              FROM station_flows sf
              JOIN external_data.dublin_bikes_stations st ON sf.station_id = st.station_id
          )
          SELECT
              o.station_id                                                                      AS origin_station_id,
              o.name                                                                            AS origin_name,
              o.latitude                                                                        AS origin_lat,
              o.longitude                                                                       AS origin_lon,
              d.station_id                                                                      AS dest_station_id,
              d.name                                                                            AS dest_name,
              d.latitude                                                                        AS dest_lat,
              d.longitude                                                                       AS dest_lon,
              GREATEST(ROUND(SQRT(o.departures * d.arrivals))::int, 1)                         AS estimated_trips,
              SQRT(POWER((o.latitude::float - d.latitude::float), 2)
                 + POWER((o.longitude::float - d.longitude::float)
                     * COS(RADIANS((o.latitude::float + d.latitude::float) / 2.0)), 2)
                 ) * 111.0                                                                    AS distance_km
          FROM station_info o
          JOIN station_info d ON o.station_id != d.station_id
            AND SQRT(POWER((o.latitude::float - d.latitude::float), 2)
                   + POWER((o.longitude::float - d.longitude::float)
                       * COS(RADIANS((o.latitude::float + d.latitude::float) / 2.0)), 2)
                   ) * 111.0 <= 5.0
          ORDER BY estimated_trips DESC
          LIMIT :limitVal
          """,
      nativeQuery = true)
  List<Object[]> findODPairs(@Param("days") int days, @Param("limitVal") int limit);
}
