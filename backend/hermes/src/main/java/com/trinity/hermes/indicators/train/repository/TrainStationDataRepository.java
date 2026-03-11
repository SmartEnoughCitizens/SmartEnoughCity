package com.trinity.hermes.indicators.train.repository;

import com.trinity.hermes.indicators.train.entity.TrainStationData;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainStationDataRepository extends JpaRepository<TrainStationData, Integer> {

  /**
   * Latest station-data record per (station_code, train_code) — one row per currently-expected
   * train at each station.
   */
  @Query(
      """
            SELECT sd FROM TrainStationData sd
            WHERE sd.fetchedAt = (
                SELECT MAX(sd2.fetchedAt) FROM TrainStationData sd2
                WHERE sd2.stationCode = sd.stationCode
                  AND sd2.trainCode  = sd.trainCode
            )
            ORDER BY sd.stationCode, sd.dueInMinutes
            """)
  List<TrainStationData> findLatestPerStationTrain();

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
   * Per-station utilization: latest snapshot only, joined to TrainStation for coordinates, filtered
   * to the Greater Dublin Area bounding box.
   *
   * <p>Returns rows of [stationCode, stationDesc, lat, lon, serviceCount, avgDelayMinutes] ordered
   * by serviceCount DESC so the busiest stations appear first.
   */
  @Query(
      """
            SELECT sd.stationCode,
                   s.stationDesc,
                   s.lat,
                   s.lon,
                   COUNT(sd) AS serviceCount,
                   COALESCE(AVG(sd.lateMinutes), 0.0) AS avgDelay
            FROM TrainStationData sd, TrainStation s
            WHERE sd.stationCode = s.stationCode
              AND sd.fetchedAt = (
                  SELECT MAX(sd2.fetchedAt)
                  FROM TrainStationData sd2
                  WHERE sd2.stationCode = sd.stationCode
                    AND sd2.trainCode   = sd.trainCode
              )
              AND s.lat BETWEEN :latMin AND :latMax
              AND s.lon BETWEEN :lonMin AND :lonMax
            GROUP BY sd.stationCode, s.stationDesc, s.lat, s.lon
            ORDER BY COUNT(sd) DESC
            """)
  List<Object[]> findStationUtilization(
      @Param("latMin") double latMin,
      @Param("latMax") double latMax,
      @Param("lonMin") double lonMin,
      @Param("lonMax") double lonMax);

  /**
   * Recurring delay patterns aggregated by station, route (origin→destination), train type, and
   * time-of-day bucket.
   *
   * <p>Uses a native PostgreSQL query so that {@code EXTRACT(HOUR FROM sch_arrival)} can be used in
   * both the SELECT and GROUP BY without repeating a full CASE expression via JPQL.
   *
   * <p>Returns rows of: [stationCode, stationDesc, lat, lon, origin, destination, trainType,
   * timeOfDay, avgDelayMinutes, maxDelayMinutes, occurrenceCount, latePercent]
   *
   * <p>Only patterns with {@code avg(late_minutes) >= 1} are returned, so purely on-time patterns
   * are excluded. Results are ordered worst-average-delay first, capped at 200 rows.
   */
  @Query(
      value =
          """
          SELECT
              sd.station_code                                                     AS stationCode,
              s.station_desc                                                      AS stationDesc,
              s.lat,
              s.lon,
              sd.origin,
              sd.destination,
              sd.train_type                                                       AS trainType,
              CASE
                  WHEN EXTRACT(HOUR FROM sd.sch_arrival) >= 6
                   AND EXTRACT(HOUR FROM sd.sch_arrival) < 9  THEN 'MORNING_PEAK'
                  WHEN EXTRACT(HOUR FROM sd.sch_arrival) >= 9
                   AND EXTRACT(HOUR FROM sd.sch_arrival) < 13 THEN 'MIDDAY'
                  WHEN EXTRACT(HOUR FROM sd.sch_arrival) >= 13
                   AND EXTRACT(HOUR FROM sd.sch_arrival) < 17 THEN 'AFTERNOON'
                  WHEN EXTRACT(HOUR FROM sd.sch_arrival) >= 17
                   AND EXTRACT(HOUR FROM sd.sch_arrival) < 21 THEN 'EVENING_PEAK'
                  ELSE 'NIGHT'
              END                                                                 AS timeOfDay,
              ROUND(AVG(sd.late_minutes)::NUMERIC, 1)                            AS avgDelayMinutes,
              MAX(sd.late_minutes)                                                AS maxDelayMinutes,
              COUNT(*)                                                            AS occurrenceCount,
              ROUND(
                  (SUM(CASE WHEN sd.late_minutes > 0 THEN 1 ELSE 0 END)::FLOAT
                   / NULLIF(COUNT(*), 0)::FLOAT * 100)::NUMERIC,
                  1)                                                              AS latePercent
          FROM external_data.irish_rail_station_data  sd
          JOIN external_data.irish_rail_station        s  ON s.station_code = sd.station_code
          WHERE sd.late_minutes IS NOT NULL
            AND sd.sch_arrival  IS NOT NULL
            AND sd.train_date   >= :fromDate
            AND s.lat BETWEEN :latMin AND :latMax
            AND s.lon BETWEEN :lonMin AND :lonMax
          GROUP BY
              sd.station_code, s.station_desc, s.lat, s.lon,
              sd.origin, sd.destination, sd.train_type,
              CASE
                  WHEN EXTRACT(HOUR FROM sd.sch_arrival) >= 6
                   AND EXTRACT(HOUR FROM sd.sch_arrival) < 9  THEN 'MORNING_PEAK'
                  WHEN EXTRACT(HOUR FROM sd.sch_arrival) >= 9
                   AND EXTRACT(HOUR FROM sd.sch_arrival) < 13 THEN 'MIDDAY'
                  WHEN EXTRACT(HOUR FROM sd.sch_arrival) >= 13
                   AND EXTRACT(HOUR FROM sd.sch_arrival) < 17 THEN 'AFTERNOON'
                  WHEN EXTRACT(HOUR FROM sd.sch_arrival) >= 17
                   AND EXTRACT(HOUR FROM sd.sch_arrival) < 21 THEN 'EVENING_PEAK'
                  ELSE 'NIGHT'
              END
          HAVING AVG(sd.late_minutes) >= 1
          ORDER BY AVG(sd.late_minutes) DESC
          LIMIT 200
          """,
      nativeQuery = true)
  List<Object[]> findDelayPatterns(
      @Param("fromDate") LocalDate fromDate,
      @Param("latMin") double latMin,
      @Param("latMax") double latMax,
      @Param("lonMin") double lonMin,
      @Param("lonMax") double lonMax);
}
