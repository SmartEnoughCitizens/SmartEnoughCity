package com.trinity.hermes.indicators.cycle.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Computes cycle coverage gaps and stores them in backend.cycle_coverage_gaps.
 *
 * <p>Joins ev_charging_demand (dwelling counts per electoral division) with small_areas (geometry)
 * to derive ED-level polygons, then finds the nearest Dublin Bikes station for each area and
 * categorises coverage.
 *
 * <p>Categories:
 *
 * <ul>
 *   <li>NO_COVERAGE — flat/apartment count > 50 AND nearest station > 1000 m
 *   <li>POOR_COVERAGE — flat/apartment count > 50 AND nearest station 500–1000 m
 *   <li>ADEQUATE — all other areas
 * </ul>
 *
 * <p>Runs on startup and nightly at 03:00. processed_for_implementation / processed_at are never
 * overwritten on refresh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoverageGapSchedulerService {

  private final JdbcTemplate jdbcTemplate;

  private static final String CREATE_TABLE_SQL =
      """
      CREATE TABLE IF NOT EXISTS backend.cycle_coverage_gaps (
          electoral_division           VARCHAR(200)      PRIMARY KEY,
          flat_apartment_count         INTEGER           NOT NULL,
          house_bungalow_count         INTEGER           NOT NULL,
          total_dwellings              INTEGER           NOT NULL,
          centroid_lat                 DOUBLE PRECISION  NOT NULL,
          centroid_lon                 DOUBLE PRECISION  NOT NULL,
          min_distance_m               DOUBLE PRECISION,
          coverage_category            VARCHAR(30)       NOT NULL,
          priority_score               INTEGER           NOT NULL,
          computed_at                  TIMESTAMPTZ       NOT NULL,
          processed_for_implementation BOOLEAN           NOT NULL DEFAULT FALSE,
          processed_at                 TIMESTAMPTZ,
          processed_by                 VARCHAR(100),
          geom_geojson                 TEXT
      )
      """;

  private static final String UPSERT_SQL =
      """
      INSERT INTO backend.cycle_coverage_gaps (
          electoral_division,
          flat_apartment_count, house_bungalow_count, total_dwellings,
          centroid_lat, centroid_lon,
          min_distance_m,
          coverage_category, priority_score, computed_at,
          geom_geojson
      )
      WITH ed_geometry AS (
          SELECT
              ev.electoral_division,
              ev.flat_apartment_count,
              ev.house_bungalow_count,
              ev.total_dwellings,
              ST_Union(sa.geom) AS geom
          FROM external_data.ev_charging_demand ev
          JOIN external_data.small_areas sa
              ON UPPER(sa.ed_name) = UPPER(SPLIT_PART(ev.electoral_division, ',', 1))
          GROUP BY
              ev.electoral_division,
              ev.flat_apartment_count,
              ev.house_bungalow_count,
              ev.total_dwellings
      ),
      nearest_station AS (
          SELECT
              eg.electoral_division,
              eg.flat_apartment_count,
              eg.house_bungalow_count,
              eg.total_dwellings,
              eg.geom,
              MIN(
                  ST_Distance(
                      ST_Transform(ST_SetSRID(ST_Centroid(eg.geom), 2157), 4326)::geography,
                      ST_SetSRID(ST_MakePoint(st.longitude::float, st.latitude::float), 4326)::geography
                  )
              ) AS min_distance_m
          FROM ed_geometry eg
          CROSS JOIN external_data.dublin_bikes_stations st
          GROUP BY
              eg.electoral_division,
              eg.flat_apartment_count,
              eg.house_bungalow_count,
              eg.total_dwellings,
              eg.geom
      )
      SELECT
          electoral_division,
          flat_apartment_count,
          house_bungalow_count,
          total_dwellings,
          ST_Y(ST_Transform(ST_SetSRID(ST_Centroid(geom), 2157), 4326))::double precision,
          ST_X(ST_Transform(ST_SetSRID(ST_Centroid(geom), 2157), 4326))::double precision,
          min_distance_m::double precision,
          CASE
              WHEN flat_apartment_count > 50 AND min_distance_m > 3000 THEN 'NO_COVERAGE'
              WHEN flat_apartment_count > 50 AND min_distance_m > 1000 THEN 'POOR_COVERAGE'
              WHEN flat_apartment_count > 50 AND min_distance_m > 500  THEN 'PARTIAL_COVERAGE'
              ELSE                                                           'ADEQUATE'
          END,
          ROUND(
              CASE
                  WHEN min_distance_m > 3000 THEN flat_apartment_count::float
                  WHEN min_distance_m > 1000 THEN flat_apartment_count::float * 0.75
                  WHEN min_distance_m > 500  THEN flat_apartment_count::float * 0.5
                  ELSE 0
              END
          )::int,
          NOW(),
          ST_AsGeoJSON(ST_Transform(ST_SetSRID(ST_SimplifyPreserveTopology(geom, 5), 2157), 4326), 4)
      FROM nearest_station
      ON CONFLICT (electoral_division) DO UPDATE SET
          flat_apartment_count  = EXCLUDED.flat_apartment_count,
          house_bungalow_count  = EXCLUDED.house_bungalow_count,
          total_dwellings       = EXCLUDED.total_dwellings,
          centroid_lat          = EXCLUDED.centroid_lat,
          centroid_lon          = EXCLUDED.centroid_lon,
          min_distance_m        = EXCLUDED.min_distance_m,
          coverage_category     = EXCLUDED.coverage_category,
          priority_score        = EXCLUDED.priority_score,
          computed_at           = EXCLUDED.computed_at,
          geom_geojson          = EXCLUDED.geom_geojson
      """;

  @PostConstruct
  public void init() {
    jdbcTemplate.execute(CREATE_TABLE_SQL);
    log.info("cycle_coverage_gaps table ready");
    computeAndStore();
  }

  @Scheduled(cron = "0 0 3 * * *") // nightly at 03:00
  public void scheduledRefresh() {
    computeAndStore();
  }

  private void computeAndStore() {
    log.info("Coverage gap analysis: starting computation...");
    try {
      int rows = jdbcTemplate.update(UPSERT_SQL);
      log.info("Coverage gap analysis: {} electoral divisions computed and stored", rows);
    } catch (Exception e) {
      log.error("Coverage gap analysis failed: {}", e.getMessage(), e);
    }
  }
}
