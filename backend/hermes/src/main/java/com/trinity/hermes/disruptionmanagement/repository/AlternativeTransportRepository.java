package com.trinity.hermes.disruptionmanagement.repository;

import com.trinity.hermes.disruptionmanagement.dto.AlternativeTransportResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * Unified parameterized alternative-transport query. Works for any disruption type — pass the
 * disruption's lat/lon and a search radius. Returns nearby bus stops, Irish Rail stations, and
 * DublinBikes stations (with available bikes) sorted by distance.
 */
@Repository
@Slf4j
public class AlternativeTransportRepository {

  private static final String NEARBY_SQL =
      """
      WITH bus AS (
          SELECT
              'bus'           AS transport_type,
              bs.id           AS stop_id,
              bs.name         AS stop_name,
              bs.lat,
              bs.lon,
              NULL::integer   AS available_bikes,
              NULL::integer   AS capacity,
              EARTH_DISTANCE(
                  LL_TO_EARTH(:lat, :lon),
                  LL_TO_EARTH(bs.lat, bs.lon)
              )::integer      AS distance_m
          FROM external_data.bus_stops bs
          WHERE EARTH_DISTANCE(
              LL_TO_EARTH(:lat, :lon),
              LL_TO_EARTH(bs.lat, bs.lon)
          ) <= :radius_m
      ),
      rail AS (
          SELECT
              'rail'               AS transport_type,
              rs.station_code      AS stop_id,
              rs.station_desc      AS stop_name,
              rs.lat,
              rs.lon,
              NULL::integer        AS available_bikes,
              NULL::integer        AS capacity,
              EARTH_DISTANCE(
                  LL_TO_EARTH(:lat, :lon),
                  LL_TO_EARTH(rs.lat, rs.lon)
              )::integer           AS distance_m
          FROM external_data.irish_rail_stations rs
          WHERE EARTH_DISTANCE(
              LL_TO_EARTH(:lat, :lon),
              LL_TO_EARTH(rs.lat, rs.lon)
          ) <= :radius_m
      ),
      bikes AS (
          SELECT
              'bike'                             AS transport_type,
              dbs.system_id                      AS stop_id,
              dbs.name                           AS stop_name,
              dbs.latitude::double precision     AS lat,
              dbs.longitude::double precision    AS lon,
              snap.available_bikes,
              dbs.capacity,
              EARTH_DISTANCE(
                  LL_TO_EARTH(:lat, :lon),
                  LL_TO_EARTH(
                      dbs.latitude::double precision,
                      dbs.longitude::double precision
                  )
              )::integer                         AS distance_m
          FROM external_data.dublin_bikes_stations dbs
          JOIN (
              SELECT DISTINCT ON (station_id)
                  station_id, available_bikes
              FROM external_data.dublin_bikes_station_snapshots
              WHERE is_renting = true
              ORDER BY station_id, timestamp DESC
          ) snap ON snap.station_id = dbs.station_id
          WHERE EARTH_DISTANCE(
              LL_TO_EARTH(:lat, :lon),
              LL_TO_EARTH(
                  dbs.latitude::double precision,
                  dbs.longitude::double precision
              )
          ) <= :radius_m
            AND snap.available_bikes > 0
      )
      SELECT * FROM bus
      UNION ALL SELECT * FROM rail
      UNION ALL SELECT * FROM bikes
      ORDER BY distance_m
      LIMIT 10
      """;

  @PersistenceContext private EntityManager em;

  /**
   * Find nearby alternative transport options within {@code radiusMetres} of (lat, lon).
   *
   * @param lat latitude of the disruption
   * @param lon longitude of the disruption
   * @param radiusMetres search radius in metres (e.g. 500)
   * @return up to 10 alternatives sorted by walking distance
   */
  @SuppressWarnings("unchecked")
  public List<AlternativeTransportResult> findNearby(double lat, double lon, int radiusMetres) {
    try {
      List<Object[]> rows =
          em.createNativeQuery(NEARBY_SQL)
              .setParameter("lat", lat)
              .setParameter("lon", lon)
              .setParameter("radius_m", radiusMetres)
              .getResultList();

      List<AlternativeTransportResult> results = new ArrayList<>(rows.size());
      for (Object[] r : rows) {
        results.add(
            new AlternativeTransportResult(
                str(r[0]), // transport_type
                str(r[1]), // stop_id
                str(r[2]), // stop_name
                toDouble(r[3]), // lat
                toDouble(r[4]), // lon
                toInt(r[6]), // available_bikes (nullable)
                toInt(r[7]), // capacity (nullable)
                toInt(r[8]) // distance_m
                ));
      }
      return results;
    } catch (Exception e) {
      log.warn("Alternative transport query failed: {}", e.getMessage());
      return List.of();
    }
  }

  private static String str(Object o) {
    return o != null ? o.toString() : null;
  }

  private static Double toDouble(Object o) {
    return o instanceof Number n ? n.doubleValue() : null;
  }

  private static Integer toInt(Object o) {
    return o instanceof Number n ? n.intValue() : null;
  }
}
