package com.trinity.hermes.indicators.train.repository;

import com.trinity.hermes.indicators.train.entity.TrainCurrentTrain;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainCurrentTrainRepository
        extends JpaRepository<TrainCurrentTrain, Integer> {

    /**
     * Returns the most-recent record per train_code that has a known location,
     * fetched today (Dublin time).
     */
    @Query("""
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

    /** Count of trains with a known position (proxy for "running now"). */
    @Query("""
            SELECT COUNT(DISTINCT t.trainCode) FROM TrainCurrentTrain t
            WHERE t.lat IS NOT NULL AND t.lon IS NOT NULL
            """)
    long countActiveTrains();
}
