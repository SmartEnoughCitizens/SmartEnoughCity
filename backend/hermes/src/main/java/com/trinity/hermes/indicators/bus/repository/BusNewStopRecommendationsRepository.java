package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusRoute;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BusNewStopRecommendationsRepository extends JpaRepository<BusRoute, String> {

  @Query(
      value =
          """
          SELECT
              rec.route_id AS routeId,
              r.short_name AS routeShortName,
              r.long_name AS routeLongName,
              rec.stop_a_id AS stopAId,
              sa.code AS stopACode,
              sa.name AS stopAName,
              sa.lat AS stopALat,
              sa.lon AS stopALon,
              rec.stop_b_id AS stopBId,
              sb.code AS stopBCode,
              sb.name AS stopBName,
              sb.lat AS stopBLat,
              sb.lon AS stopBLon,
              rec.candidate_lat::float8 AS candidateLat,
              rec.candidate_lon::float8 AS candidateLon,
              rec.population_score::float8 AS populationScore,
              rec.public_space_score::float8 AS publicSpaceScore,
              rec.combined_score::float8 AS combinedScore
          FROM backend.bus_new_stops_recommendations rec
          INNER JOIN external_data.bus_routes r ON r.id = rec.route_id
          INNER JOIN external_data.bus_stops sa ON sa.id = rec.stop_a_id
          INNER JOIN external_data.bus_stops sb ON sb.id = rec.stop_b_id
          ORDER BY rec.combined_score DESC
          LIMIT 20
          """,
      nativeQuery = true)
  List<BusNewStopRecommendationProjection> findTop20ByCombinedScoreDesc();
}
