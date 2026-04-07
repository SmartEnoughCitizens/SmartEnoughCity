package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusStopRepository extends JpaRepository<BusStop, String> {

  @Query(
      "SELECT s FROM BusStop s WHERE s.lat BETWEEN :minLat AND :maxLat"
          + " AND s.lon BETWEEN :minLon AND :maxLon")
  List<BusStop> findStopsInBounds(
      @Param("minLat") Double minLat,
      @Param("maxLat") Double maxLat,
      @Param("minLon") Double minLon,
      @Param("maxLon") Double maxLon);

  /**
   * Returns [route_id, short_name] for distinct bus routes whose GTFS stops lie within {@code
   * radiusM} metres of (lat, lon). Uses PostGIS earth_distance for accuracy.
   */
  @Query(
      value =
          "SELECT DISTINCT bt.route_id, br.short_name"
              + " FROM external_data.bus_stops bs"
              + " JOIN external_data.bus_stop_times bst ON bst.stop_id = bs.id"
              + " JOIN external_data.bus_trips bt ON bst.trip_id = bt.id"
              + " JOIN external_data.bus_routes br ON bt.route_id = br.id"
              + " WHERE public.EARTH_DISTANCE(public.LL_TO_EARTH(:lat, :lon), public.LL_TO_EARTH(bs.lat, bs.lon))"
              + "     <= :radiusM"
              + " ORDER BY br.short_name"
              + " LIMIT 20",
      nativeQuery = true)
  List<Object[]> findRouteShortNamesNear(
      @Param("lat") double lat, @Param("lon") double lon, @Param("radiusM") int radiusM);
}
