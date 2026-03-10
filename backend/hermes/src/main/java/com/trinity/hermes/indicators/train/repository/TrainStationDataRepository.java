package com.trinity.hermes.indicators.train.repository;

import com.trinity.hermes.indicators.train.entity.TrainStationData;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
