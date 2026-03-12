package com.trinity.hermes.indicators.cycle.repository;

import com.trinity.hermes.indicators.cycle.entity.DublinBikesSnapshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
              COUNT(*)                                                          AS total_stations,
              SUM(l.available_bikes)                                            AS total_bikes,
              SUM(l.available_docks)                                            AS total_docks,
              COALESCE(SUM(l.disabled_bikes), 0)                               AS disabled_bikes,
              COALESCE(SUM(l.disabled_docks), 0)                               AS disabled_docks,
              COUNT(CASE WHEN l.available_bikes = 0 AND l.is_installed THEN 1 END) AS empty_stations,
              COUNT(CASE WHEN l.available_docks = 0 AND l.is_installed THEN 1 END) AS full_stations,
              AVG((st.capacity - l.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS avg_fullness_pct,
              MAX(l.timestamp)                                                  AS latest_timestamp
          FROM latest l
          JOIN external_data.dublin_bikes_stations st ON l.station_id = st.station_id
          WHERE l.is_installed = true
          """,
      nativeQuery = true)
  Object[] findNetworkSummary();

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
  Object[] findNetworkImbalanceScore();
}
