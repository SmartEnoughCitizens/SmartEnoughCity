package com.trinity.hermes.indicators.train.repository;

import com.trinity.hermes.indicators.train.entity.TrainCurrentTrain;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainCurrentTrainRepository extends JpaRepository<TrainCurrentTrain, Integer> {

  /**
   * Returns the most-recent record per train_code that has a known location (all Ireland). Kept for
   * backward-compatibility / admin use.
   */
  @Query(
      """
          SELECT t FROM TrainCurrentTrain t
          WHERE t.lat IS NOT NULL
            AND t.lon IS NOT NULL
            AND t.fetchedAt = (
                SELECT MAX(t2.fetchedAt) FROM TrainCurrentTrain t2
                WHERE t2.trainCode = t.trainCode
            )
          ORDER BY t.trainCode
          """)
  List<TrainCurrentTrain> findLatestPositionPerTrain();

  /** Count of trains with a known position across all Ireland. */
  @Query(
      """
          SELECT COUNT(DISTINCT t.trainCode) FROM TrainCurrentTrain t
          WHERE t.lat IS NOT NULL AND t.lon IS NOT NULL
          """)
  long countActiveTrains();

  /**
   * Latest train positions within the Greater Dublin Area bounding box.
   *
   * <p>Bounding box: Greystones (south) → Drogheda (north), Maynooth (west) → coast (east).
   */
  @Query(
      """
          SELECT t FROM TrainCurrentTrain t
          WHERE t.lat IS NOT NULL
            AND t.lon IS NOT NULL
            AND t.lat BETWEEN :latMin AND :latMax
            AND t.lon BETWEEN :lonMin AND :lonMax
            AND t.fetchedAt = (
                SELECT MAX(t2.fetchedAt) FROM TrainCurrentTrain t2
                WHERE t2.trainCode = t.trainCode
            )
          ORDER BY t.trainCode
          """)
  List<TrainCurrentTrain> findLatestDublinTrainPositions(
      @Param("latMin") double latMin,
      @Param("latMax") double latMax,
      @Param("lonMin") double lonMin,
      @Param("lonMax") double lonMax);

  /**
   * Count of active trains within the Greater Dublin Area bounding box.
   */
  @Query(
      """
          SELECT COUNT(DISTINCT t.trainCode) FROM TrainCurrentTrain t
          WHERE t.lat IS NOT NULL
            AND t.lon IS NOT NULL
            AND t.lat BETWEEN :latMin AND :latMax
            AND t.lon BETWEEN :lonMin AND :lonMax
          """)
  long countActiveDublinTrains(
      @Param("latMin") double latMin,
      @Param("latMax") double latMax,
      @Param("lonMin") double lonMin,
      @Param("lonMax") double lonMax);
}
