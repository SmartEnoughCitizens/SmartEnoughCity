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
   * One canonical ordered route polyline per route_id (direction 0 = outbound). Stops are
   * filtered to the Dublin bounding box — intercity routes with only one stop in Dublin are
   * excluded by the service layer.
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

  /**
   * Average daily trip frequency per Dublin stop across a full 7-day week,
   * enriched with 2024 ridership and the total population living within
   * 800 m of each station (PostGIS ST_DWithin on CSO small areas).
   */
  @Query(
      value =
          """
          SELECT
              sub.stop_id                                           AS stopId,
              s.name                                               AS name,
              s.lat                                                AS lat,
              s.lon                                                AS lon,
              ROUND(
                SUM(sub.trip_count *
                    ((cs.monday::int) + (cs.tuesday::int) + (cs.wednesday::int) +
                     (cs.thursday::int) + (cs.friday::int) +
                     (cs.saturday::int) + (cs.sunday::int))
                ) / 7.0
              )::int                                               AS tripCount,
              COALESCE(r.count_2024, 0)                           AS ridershipCount,
              COALESCE(pop.catchment_population, 0)               AS catchmentPopulation,
              ir.station_type                                     AS stationType
          FROM (
              SELECT st.stop_id, t.service_id,
                     COUNT(DISTINCT st.trip_id) AS trip_count
              FROM external_data.train_stop_times st
              JOIN external_data.train_trips t ON t.id = st.trip_id
              GROUP BY st.stop_id, t.service_id
          ) sub
          JOIN external_data.train_stops s
               ON s.id = sub.stop_id
          JOIN external_data.train_calendar_schedule cs
               ON cs.service_id = sub.service_id
          LEFT JOIN external_data.train_station_ridership r
               ON LOWER(TRIM(r.station)) = LOWER(TRIM(s.name))
          LEFT JOIN (
              SELECT ts2.id AS stop_id, COALESCE(SUM(sa.population), 0) AS catchment_population
              FROM external_data.train_stops ts2
              LEFT JOIN external_data.small_areas sa
                   ON ST_DWithin(ST_SetSRID(ST_MakePoint(ts2.lon, ts2.lat), 4326), sa.geom, 0.0072)
              WHERE ts2.lat BETWEEN :latMin AND :latMax
                AND ts2.lon BETWEEN :lonMin AND :lonMax
              GROUP BY ts2.id
          ) pop ON pop.stop_id = sub.stop_id
          LEFT JOIN external_data.irish_rail_stations ir
               ON LOWER(TRIM(ir.station_desc)) = LOWER(TRIM(s.name))
          WHERE s.lat BETWEEN :latMin AND :latMax
            AND s.lon BETWEEN :lonMin AND :lonMax
          GROUP BY sub.stop_id, s.name, s.lat, s.lon, r.count_2024,
                   pop.catchment_population, ir.station_type
          ORDER BY tripCount DESC
          """,
      nativeQuery = true)
  List<StationTripCountProjection> findStationTripCounts(
      @Param("latMin") double latMin,
      @Param("latMax") double latMax,
      @Param("lonMin") double lonMin,
      @Param("lonMax") double lonMax);
}
