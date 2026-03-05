package com.trinity.hermes.indicators.train.repository;

import com.trinity.hermes.indicators.train.entity.TrainStation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainStationRepository extends JpaRepository<TrainStation, Integer> {

  TrainStation findByStationCode(String stationCode);

  /**
   * Fetch all stations within the Greater Dublin Area bounding box, ordered by name.
   *
   * <p>Bounding box: Greystones (south) → Drogheda (north), Maynooth (west) → coast (east).
   */
  @Query(
      """
          SELECT s FROM TrainStation s
          WHERE s.lat BETWEEN :latMin AND :latMax
            AND s.lon BETWEEN :lonMin AND :lonMax
          ORDER BY s.stationDesc
          """)
  List<TrainStation> findAllDublinStations(
      @Param("latMin") double latMin,
      @Param("latMax") double latMax,
      @Param("lonMin") double lonMin,
      @Param("lonMax") double lonMax);

  /** Count stations within the Greater Dublin Area bounding box. */
  @Query(
      """
          SELECT COUNT(s) FROM TrainStation s
          WHERE s.lat BETWEEN :latMin AND :latMax
            AND s.lon BETWEEN :lonMin AND :lonMax
          """)
  long countDublinStations(
      @Param("latMin") double latMin,
      @Param("latMax") double latMax,
      @Param("lonMin") double lonMin,
      @Param("lonMax") double lonMax);
}
