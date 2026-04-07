package com.trinity.hermes.indicators.train.repository;

import com.trinity.hermes.indicators.train.entity.GtfsStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GtfsStopRepository extends JpaRepository<GtfsStop, String> {

  /**
   * Dublin-area stops from GTFS, enriched with station_type from irish_rail_stations via a loose
   * name match. Falls back gracefully to null station_type when no match exists.
   */
  @Query(
      value =
          """
          SELECT
              s.id          AS id,
              s.name        AS name,
              s.lat         AS lat,
              s.lon         AS lon,
              ir.station_type AS stationType
          FROM external_data.train_stops s
          LEFT JOIN external_data.irish_rail_stations ir
               ON LOWER(TRIM(ir.station_desc)) = LOWER(TRIM(s.name))
          WHERE s.lat BETWEEN :latMin AND :latMax
            AND s.lon BETWEEN :lonMin AND :lonMax
          ORDER BY s.name
          """,
      nativeQuery = true)
  List<GtfsDublinStopProjection> findDublinStops(
      @Param("latMin") double latMin,
      @Param("latMax") double latMax,
      @Param("lonMin") double lonMin,
      @Param("lonMax") double lonMax);

  /** Count of Dublin-area GTFS stops — used to decide whether to fall back. */
  @Query(
      value =
          """
          SELECT COUNT(*) FROM external_data.train_stops
          WHERE lat BETWEEN :latMin AND :latMax AND lon BETWEEN :lonMin AND :lonMax
          """,
      nativeQuery = true)
  long countDublinStops(
      @Param("latMin") double latMin,
      @Param("latMax") double latMax,
      @Param("lonMin") double lonMin,
      @Param("lonMax") double lonMax);

  /**
   * One canonical ordered route polyline per route_id (direction 0 = outbound). Stops are filtered
   * to the Dublin bounding box — intercity routes with only one stop in Dublin are excluded by the
   * service layer.
   */
  @Query(
      value =
          """
          SELECT
              r.id          AS routeId,
              r.long_name   AS routeName,
              r.short_name  AS shortName,
              s.id          AS stopId,
              st.sequence   AS locationOrder,
              s.lat         AS lat,
              s.lon         AS lon
          FROM (
              SELECT DISTINCT ON (route_id) id, route_id
              FROM external_data.train_trips
              WHERE direction_id = 0
              ORDER BY route_id, id
          ) t
          JOIN external_data.train_routes r  ON r.id  = t.route_id
          JOIN external_data.train_stop_times st ON st.trip_id = t.id
          JOIN external_data.train_stops      s  ON s.id  = st.stop_id
          WHERE s.lat BETWEEN :latMin AND :latMax
            AND s.lon BETWEEN :lonMin AND :lonMax
          ORDER BY r.id, st.sequence
          """,
      nativeQuery = true)
  List<GtfsRouteStopProjection> findRoutePolylines(
      @Param("latMin") double latMin,
      @Param("latMax") double latMax,
      @Param("lonMin") double lonMin,
      @Param("lonMax") double lonMax);
}
