package com.trinity.hermes.indicators.train.repository;

import com.trinity.hermes.indicators.train.entity.TrainMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainMovementRepository
    extends JpaRepository<TrainMovement, TrainMovement.TrainMovementId> {

  /**
   * Returns one representative ordered stop per (origin, destination, location_order). DISTINCT ON
   * deduplicates across multiple train_dates/codes so we get a single canonical route shape per
   * origin→destination pair. No bounding-box filter — routes may extend beyond Dublin.
   */
  @Query(
      value =
          """
          SELECT DISTINCT ON (m.train_origin, m.train_destination, m.location_order)
              m.train_origin      AS trainOrigin,
              m.train_destination AS trainDestination,
              m.location_order    AS locationOrder,
              s.lat               AS lat,
              s.lon               AS lon
          FROM external_data.irish_rail_train_movements m
          JOIN external_data.irish_rail_stations s ON s.station_code = m.location_code
          WHERE s.lat IS NOT NULL AND s.lon IS NOT NULL
          ORDER BY m.train_origin, m.train_destination, m.location_order
          """,
      nativeQuery = true)
  List<TrainRouteStopProjection> findAllRouteStops();
}
