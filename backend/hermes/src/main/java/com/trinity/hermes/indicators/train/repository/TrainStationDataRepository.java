package com.trinity.hermes.indicators.train.repository;

import com.trinity.hermes.indicators.train.entity.TrainStationData;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainStationDataRepository extends JpaRepository<TrainStationData, Integer> {

  /**
   * Latest station-data record per (station_code, train_code) — one row per currently-expected
   * train at each station. Restricted to Dublin area stations (lat/lon bounding box).
   */
  @Query(
      value =
          """
          SELECT sd.* FROM external_data.irish_rail_station_data sd
          JOIN external_data.irish_rail_stations s ON s.station_code = sd.station_code
          WHERE sd.fetched_at = (
              SELECT MAX(sd2.fetched_at) FROM external_data.irish_rail_station_data sd2
              WHERE sd2.station_code = sd.station_code
                AND sd2.train_code   = sd.train_code
          )
          AND s.lat BETWEEN :latMin AND :latMax
          AND s.lon BETWEEN :lonMin AND :lonMax
          ORDER BY sd.station_code, sd.due_in_minutes
          """,
      nativeQuery = true)
  List<TrainStationData> findLatestPerStationTrain(
      @Param("latMin") double latMin,
      @Param("latMax") double latMax,
      @Param("lonMin") double lonMin,
      @Param("lonMax") double lonMax);

  /** Average delay in minutes across all recent station-data records. */
  @Query(
      """
            SELECT COALESCE(AVG(sd.lateMinutes), 0.0)
            FROM TrainStationData sd
            WHERE sd.lateMinutes IS NOT NULL
            """)
  Double findAverageLateMinutes();

  /** Fraction of records where lateMinutes > 0 (late arrival %). Returns a value in [0, 100]. */
  @Query(
      """
            SELECT COALESCE(
                CAST(SUM(CASE WHEN sd.lateMinutes > 0 THEN 1 ELSE 0 END) AS double) /
                NULLIF(COUNT(sd), 0) * 100.0,
                0.0)
            FROM TrainStationData sd
            """)
  Double findLateArrivalPct();

  /** Average due-in minutes across all recent station-data records. */
  @Query(
      """
            SELECT COALESCE(AVG(sd.dueInMinutes), 0.0)
            FROM TrainStationData sd
            WHERE sd.dueInMinutes IS NOT NULL
            """)
  Double findAverageDueInMinutes();

  /**
   * Frequently delayed trains — one row per unique (train_code, origin, destination, direction).
   *
   * <p>Method:
   *
   * <ol>
   *   <li>CTE computes the average delay per stop per train (excludes ORIGIN stops).
   *   <li>Outer query sums those per-stop averages into a total average delay for the train.
   * </ol>
   */
  @Query(
      value =
          """
          WITH per_stop_avg AS (
              SELECT
                  train_code,
                  station_code,
                  origin,
                  destination,
                  COALESCE(direction, 'Unknown') AS direction,
                  AVG(late_minutes)              AS avg_delay_at_stop
              FROM external_data.irish_rail_station_data
              WHERE late_minutes   IS NOT NULL
                AND location_type != 'ORIGIN'
              GROUP BY train_code, station_code, origin, destination,
                       COALESCE(direction, 'Unknown')
          )
          SELECT
              train_code                                          AS train_code,
              origin,
              destination,
              direction,
              ROUND(CAST(SUM(avg_delay_at_stop) AS numeric), 2)  AS total_avg_delay_minutes
          FROM per_stop_avg
          GROUP BY train_code, origin, destination, direction
          ORDER BY total_avg_delay_minutes DESC
          """,
      nativeQuery = true)
  List<TrainDelayProjection> findFrequentlyDelayedTrains();

  /**
   * One lat/lon per (origin, destination, station_code) — one stop per route. Uses GROUP BY so
   * there are no DISTINCT ON ordering quirks. Ordered by earliest scheduled time so the polyline
   * follows the actual travel sequence.
   */
  @Query(
      value =
          """
          SELECT
              sd.origin      AS trainOrigin,
              sd.destination AS trainDestination,
              CAST(MIN(COALESCE(sd.sch_depart, sd.sch_arrival)) AS TEXT) AS schDepart,
              s.lat          AS lat,
              s.lon          AS lon
          FROM external_data.irish_rail_station_data sd
          JOIN external_data.irish_rail_stations s ON s.station_code = sd.station_code
          WHERE s.lat  IS NOT NULL
            AND s.lon  IS NOT NULL
            AND sd.origin      IS NOT NULL
            AND sd.destination IS NOT NULL
          GROUP BY sd.origin, sd.destination, sd.station_code, s.lat, s.lon
          ORDER BY sd.origin, sd.destination, MIN(COALESCE(sd.sch_depart, sd.sch_arrival))
          """,
      nativeQuery = true)
  List<TrainRouteStopProjection> findRouteStopsViaStationData();
}
