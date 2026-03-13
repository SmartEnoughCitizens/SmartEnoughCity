package com.trinity.hermes.indicators.cycle.repository;

import com.trinity.hermes.indicators.cycle.entity.DublinBikesHistory;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DublinBikesHistoryRepository extends JpaRepository<DublinBikesHistory, Integer> {

  // -------------------------------------------------------------------------
  // Time-series per station
  // -------------------------------------------------------------------------

  /**
   * Hourly aggregated time series for a single station. Returns Object[] rows: period,
   * avg_available_bikes, avg_available_docks, usage_rate_pct
   */
  @Query(
      value =
          """
          SELECT
              DATE_TRUNC('hour', h.timestamp)                                        AS period,
              AVG(h.available_bikes)                                                 AS avg_available_bikes,
              AVG(h.available_docks)                                                 AS avg_available_docks,
              AVG((st.capacity - h.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS usage_rate_pct
          FROM external_data.dublin_bikes_station_history h
          JOIN external_data.dublin_bikes_stations st ON h.station_id = st.station_id
          WHERE h.station_id = :stationId
            AND h.timestamp BETWEEN :from AND :to
          GROUP BY period
          ORDER BY period
          """,
      nativeQuery = true)
  List<Object[]> findHourlyTimeSeriesForStation(
      @Param("stationId") Integer stationId, @Param("from") Instant from, @Param("to") Instant to);

  /**
   * Daily aggregated time series for a single station. Returns Object[] rows: period,
   * avg_available_bikes, avg_available_docks, usage_rate_pct
   */
  @Query(
      value =
          """
          SELECT
              DATE_TRUNC('day', h.timestamp)                                         AS period,
              AVG(h.available_bikes)                                                 AS avg_available_bikes,
              AVG(h.available_docks)                                                 AS avg_available_docks,
              AVG((st.capacity - h.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS usage_rate_pct
          FROM external_data.dublin_bikes_station_history h
          JOIN external_data.dublin_bikes_stations st ON h.station_id = st.station_id
          WHERE h.station_id = :stationId
            AND h.timestamp BETWEEN :from AND :to
          GROUP BY period
          ORDER BY period
          """,
      nativeQuery = true)
  List<Object[]> findDailyTimeSeriesForStation(
      @Param("stationId") Integer stationId, @Param("from") Instant from, @Param("to") Instant to);

  /**
   * Weekly aggregated time series for a single station. Returns Object[] rows: period,
   * avg_available_bikes, avg_available_docks, usage_rate_pct
   */
  @Query(
      value =
          """
          SELECT
              DATE_TRUNC('week', h.timestamp)                                        AS period,
              AVG(h.available_bikes)                                                 AS avg_available_bikes,
              AVG(h.available_docks)                                                 AS avg_available_docks,
              AVG((st.capacity - h.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS usage_rate_pct
          FROM external_data.dublin_bikes_station_history h
          JOIN external_data.dublin_bikes_stations st ON h.station_id = st.station_id
          WHERE h.station_id = :stationId
            AND h.timestamp BETWEEN :from AND :to
          GROUP BY period
          ORDER BY period
          """,
      nativeQuery = true)
  List<Object[]> findWeeklyTimeSeriesForStation(
      @Param("stationId") Integer stationId, @Param("from") Instant from, @Param("to") Instant to);

  // -------------------------------------------------------------------------
  // Network-wide trends
  // -------------------------------------------------------------------------

  /**
   * Hourly usage profile across the network (0–23). Returns Object[] rows: hour_of_day (int),
   * avg_usage_rate (double)
   */
  @Query(
      value =
          """
          SELECT
              EXTRACT(HOUR FROM h.timestamp)::int                                   AS hour_of_day,
              AVG((st.capacity - h.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS avg_usage_rate
          FROM external_data.dublin_bikes_station_history h
          JOIN external_data.dublin_bikes_stations st ON h.station_id = st.station_id
          WHERE h.timestamp >= :since
          GROUP BY hour_of_day
          ORDER BY hour_of_day
          """,
      nativeQuery = true)
  List<Object[]> findHourlyUsageProfile(@Param("since") Instant since);

  /**
   * Day-of-week usage profile across the network (0=Sunday … 6=Saturday). Returns Object[] rows:
   * day_of_week (int), avg_usage_rate (double)
   */
  @Query(
      value =
          """
          SELECT
              EXTRACT(DOW FROM h.timestamp)::int                                    AS day_of_week,
              AVG((st.capacity - h.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS avg_usage_rate
          FROM external_data.dublin_bikes_station_history h
          JOIN external_data.dublin_bikes_stations st ON h.station_id = st.station_id
          WHERE h.timestamp >= :since
          GROUP BY day_of_week
          ORDER BY day_of_week
          """,
      nativeQuery = true)
  List<Object[]> findWeeklyUsageProfile(@Param("since") Instant since);

  /**
   * Weekday vs weekend average usage. Returns Object[] rows: day_type ('weekday'|'weekend'),
   * avg_usage_rate (double)
   */
  @Query(
      value =
          """
          SELECT
              CASE WHEN EXTRACT(DOW FROM h.timestamp) IN (0, 6) THEN 'weekend' ELSE 'weekday' END AS day_type,
              AVG((st.capacity - h.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS avg_usage_rate
          FROM external_data.dublin_bikes_station_history h
          JOIN external_data.dublin_bikes_stations st ON h.station_id = st.station_id
          WHERE h.timestamp >= :since
          GROUP BY day_type
          """,
      nativeQuery = true)
  List<Object[]> findWeekdayVsWeekendUsage(@Param("since") Instant since);

  /**
   * Daily trend for the whole network (last N days). Returns Object[] rows: period,
   * avg_available_bikes, avg_available_docks, usage_rate_pct
   */
  @Query(
      value =
          """
          SELECT
              DATE_TRUNC('day', h.timestamp)                                         AS period,
              AVG(h.available_bikes)                                                 AS avg_available_bikes,
              AVG(h.available_docks)                                                 AS avg_available_docks,
              AVG((st.capacity - h.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS usage_rate_pct
          FROM external_data.dublin_bikes_station_history h
          JOIN external_data.dublin_bikes_stations st ON h.station_id = st.station_id
          WHERE h.timestamp >= :since
          GROUP BY period
          ORDER BY period
          """,
      nativeQuery = true)
  List<Object[]> findNetworkDailyTrend(@Param("since") Instant since);

  /**
   * Monthly trend for the whole network. Returns Object[] rows: period, avg_available_bikes,
   * avg_available_docks, usage_rate_pct
   */
  @Query(
      value =
          """
          SELECT
              DATE_TRUNC('month', h.timestamp)                                       AS period,
              AVG(h.available_bikes)                                                 AS avg_available_bikes,
              AVG(h.available_docks)                                                 AS avg_available_docks,
              AVG((st.capacity - h.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS usage_rate_pct
          FROM external_data.dublin_bikes_station_history h
          JOIN external_data.dublin_bikes_stations st ON h.station_id = st.station_id
          WHERE h.timestamp >= :since
          GROUP BY period
          ORDER BY period
          """,
      nativeQuery = true)
  List<Object[]> findNetworkMonthlyTrend(@Param("since") Instant since);

  // -------------------------------------------------------------------------
  // Station rankings
  // -------------------------------------------------------------------------

  /**
   * Today's station rankings ordered by avg usage rate descending (busiest first). Window: midnight
   * UTC today → now. Returns Object[] rows: station_id, name, avg_usage_rate
   */
  @Query(
      value =
          """
          SELECT
              h.station_id,
              st.name,
              AVG((st.capacity - h.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS avg_usage_rate
          FROM external_data.dublin_bikes_station_history h
          JOIN external_data.dublin_bikes_stations st ON h.station_id = st.station_id
          WHERE h.timestamp >= DATE_TRUNC('day', NOW() AT TIME ZONE 'UTC')
          GROUP BY h.station_id, st.name
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
              h.station_id,
              st.name,
              AVG((st.capacity - h.available_docks)::float / NULLIF(st.capacity, 0) * 100) AS avg_usage_rate
          FROM external_data.dublin_bikes_station_history h
          JOIN external_data.dublin_bikes_stations st ON h.station_id = st.station_id
          WHERE h.timestamp >= DATE_TRUNC('day', NOW() AT TIME ZONE 'UTC')
          GROUP BY h.station_id, st.name
          ORDER BY avg_usage_rate ASC
          LIMIT :limitVal
          """,
      nativeQuery = true)
  List<Object[]> findLeastUsedStations(@Param("limitVal") int limit);

  // -------------------------------------------------------------------------
  // Derived KPIs
  // -------------------------------------------------------------------------

  /**
   * Estimates daily trip counts using bike count decreases per station. Returns Object[] rows:
   * period (day), trips_estimate
   */
  @Query(
      value =
          """
          WITH deltas AS (
              SELECT
                  station_id,
                  timestamp,
                  LAG(available_bikes) OVER (PARTITION BY station_id ORDER BY timestamp) - available_bikes AS bikes_taken
              FROM external_data.dublin_bikes_station_history
              WHERE timestamp BETWEEN :from AND :to
          )
          SELECT
              DATE_TRUNC('day', timestamp) AS period,
              SUM(GREATEST(bikes_taken, 0))::bigint AS trips_estimate
          FROM deltas
          WHERE bikes_taken IS NOT NULL
          GROUP BY period
          ORDER BY period
          """,
      nativeQuery = true)
  List<Object[]> findDailyTripEstimates(@Param("from") Instant from, @Param("to") Instant to);

  /**
   * Average hourly turnover rate per station (bike checkouts per hour) over the last 24 hours.
   * Returns a single Object[] with: avg_hourly_turnover
   */
  @Query(
      value =
          """
          WITH changes AS (
              SELECT
                  station_id,
                  LAG(available_bikes) OVER (PARTITION BY station_id ORDER BY timestamp) - available_bikes AS bikes_taken
              FROM external_data.dublin_bikes_station_history
              WHERE timestamp >= NOW() - INTERVAL '24 hours'
          ),
          per_station AS (
              SELECT station_id, SUM(GREATEST(bikes_taken, 0)) AS total_out
              FROM changes
              WHERE bikes_taken IS NOT NULL
              GROUP BY station_id
          )
          SELECT AVG(total_out::float / 24) AS avg_hourly_turnover FROM per_station
          """,
      nativeQuery = true)
  List<Object[]> findAvgHourlyTurnoverRate();

  /** Total trip estimate for a given day. Returns a single Object[] with: trips_estimate */
  @Query(
      value =
          """
          WITH deltas AS (
              SELECT
                  LAG(available_bikes) OVER (PARTITION BY station_id ORDER BY timestamp) - available_bikes AS bikes_taken
              FROM external_data.dublin_bikes_station_history
              WHERE timestamp BETWEEN :from AND :to
          )
          SELECT SUM(GREATEST(bikes_taken, 0))::bigint AS trips_estimate
          FROM deltas
          WHERE bikes_taken IS NOT NULL
          """,
      nativeQuery = true)
  List<Object[]> findTotalTripEstimate(@Param("from") Instant from, @Param("to") Instant to);

  // -------------------------------------------------------------------------
  // Origin-Destination pairs
  // -------------------------------------------------------------------------

  /**
   * Infers origin-destination pairs from history by correlating bike departures at one station with
   * arrivals at another within a 30-minute window. Returns Object[] rows: origin_station_id,
   * origin_name, origin_lat, origin_lon, dest_station_id, dest_name, dest_lat, dest_lon,
   * estimated_trips
   */
  @Query(
      value =
          """
          WITH changes AS (
              SELECT
                  station_id,
                  timestamp,
                  available_bikes,
                  LAG(available_bikes) OVER (PARTITION BY station_id ORDER BY timestamp) AS prev_bikes
              FROM external_data.dublin_bikes_station_history
              WHERE timestamp BETWEEN :from AND :to
          ),
          departures AS (
              SELECT station_id AS origin_station_id, timestamp AS departure_time
              FROM changes
              WHERE prev_bikes IS NOT NULL AND available_bikes < prev_bikes
          ),
          arrivals AS (
              SELECT station_id AS dest_station_id, timestamp AS arrival_time
              FROM changes
              WHERE prev_bikes IS NOT NULL AND available_bikes > prev_bikes
          )
          SELECT
              d.origin_station_id,
              os.name        AS origin_name,
              os.latitude    AS origin_lat,
              os.longitude   AS origin_lon,
              a.dest_station_id,
              ds.name        AS dest_name,
              ds.latitude    AS dest_lat,
              ds.longitude   AS dest_lon,
              COUNT(*)::bigint AS estimated_trips
          FROM departures d
          JOIN arrivals a
               ON a.arrival_time BETWEEN d.departure_time
                                     AND d.departure_time + INTERVAL '30 minutes'
              AND a.dest_station_id != d.origin_station_id
          JOIN external_data.dublin_bikes_stations os ON d.origin_station_id = os.station_id
          JOIN external_data.dublin_bikes_stations ds ON a.dest_station_id   = ds.station_id
          GROUP BY d.origin_station_id, os.name, os.latitude, os.longitude,
                   a.dest_station_id,  ds.name, ds.latitude, ds.longitude
          ORDER BY estimated_trips DESC
          LIMIT :limitVal
          """,
      nativeQuery = true)
  List<Object[]> findODPairs(
      @Param("from") Instant from, @Param("to") Instant to, @Param("limitVal") int limit);
}
