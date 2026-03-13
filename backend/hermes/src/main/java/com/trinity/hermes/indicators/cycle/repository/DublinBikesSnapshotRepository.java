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
              COUNT(*)                                                                     AS total_stations,
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
                 + POWER((s.longitude::float - e.longitude::float), 2)) * 111.0     AS distance_km
          FROM empty_stations e
          CROSS JOIN surplus_stations s
          WHERE e.station_id != s.station_id
          ORDER BY e.station_id, distance_km ASC
          LIMIT :limitVal
          """,
      nativeQuery = true)
  List<Object[]> findRebalancingSuggestions(@Param("limitVal") int limit);
}
