package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.TramStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TramStopRepository extends JpaRepository<TramStop, String> {

  List<TramStop> findByLine(String line);

  @Query("SELECT COUNT(s) FROM TramStop s")
  long countAllStops();

  @Query("SELECT COUNT(DISTINCT s.line) FROM TramStop s")
  long countDistinctLines();

  @Query("SELECT s FROM TramStop s WHERE s.line = :line ORDER BY s.name")
  List<TramStop> findByLineOrderByName(@Param("line") String line);

  /**
   * Returns [stop_id, line, name, lat, lon] for tram stops within {@code radiusM} metres of (lat,
   * lon). Used to identify which Luas lines are affected by a nearby disruption.
   */
  @Query(
      value =
          "SELECT s.stop_id, s.line, s.name, s.lat, s.lon"
              + " FROM external_data.tram_luas_stops s"
              + " WHERE public.EARTH_DISTANCE(public.LL_TO_EARTH(:lat, :lon), public.LL_TO_EARTH(s.lat, s.lon))"
              + "     <= :radiusM"
              + " ORDER BY s.line, s.name",
      nativeQuery = true)
  List<Object[]> findStopsNear(
      @Param("lat") double lat, @Param("lon") double lon, @Param("radiusM") int radiusM);
}
