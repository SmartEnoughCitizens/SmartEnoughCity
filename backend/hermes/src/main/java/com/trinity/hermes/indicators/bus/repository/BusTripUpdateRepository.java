package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusTripUpdate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusTripUpdateRepository extends JpaRepository<BusTripUpdate, Long> {

  List<BusTripUpdate> findByRouteId(String routeId);

  List<BusTripUpdate> findByRouteIdOrderByIdDesc(String routeId);

  @Query("SELECT b FROM BusTripUpdate b WHERE b.routeId = :routeId ORDER BY b.id DESC")
  List<BusTripUpdate> findLatestByRouteId(@Param("routeId") String routeId);

  List<BusTripUpdate> findByStopId(String stopId);

  @Query("SELECT DISTINCT b.routeId FROM BusTripUpdate b")
  List<String> findAllDistinctRouteIds();

  @Query(
      "SELECT AVG(b.arrivalDelay) FROM BusTripUpdate b WHERE b.routeId = :routeId AND b.arrivalDelay IS NOT NULL")
  Double findAverageArrivalDelayByRouteId(@Param("routeId") String routeId);

  @Query(
      "SELECT AVG(b.departureDelay) FROM BusTripUpdate b WHERE b.routeId = :routeId AND b.departureDelay IS NOT NULL")
  Double findAverageDepartureDelayByRouteId(@Param("routeId") String routeId);

  @Query(
      value =
          """
          SELECT
              bt.route_id   AS routeId,
              br.short_name AS routeShortName,
              br.long_name  AS routeLongName,
              ROUND(CAST(AVG(stu.arrival_delay) AS numeric) / 60.0, 2) AS avgDelayMinutes
          FROM external_data.bus_live_trip_updates_stop_time_updates stu
          JOIN external_data.bus_live_trip_updates ltu ON ltu.entry_id = stu.trip_update_entry_id
          JOIN external_data.bus_trips bt ON bt.id = ltu.trip_id
          JOIN external_data.bus_routes br ON br.id = bt.route_id
          WHERE stu.arrival_delay IS NOT NULL
            AND stu.arrival_delay > 0
            AND (
              (:filter = 'today' AND ltu.start_date = CURRENT_DATE)
              OR (:filter = 'week'  AND ltu.start_date >= CURRENT_DATE - INTERVAL '7 days')
              OR (:filter = 'month' AND ltu.start_date >= CURRENT_DATE - INTERVAL '30 days')
            )
          GROUP BY bt.route_id, br.short_name, br.long_name
          ORDER BY avgDelayMinutes DESC
          LIMIT 10
          """,
      nativeQuery = true)
  List<BusCommonDelayProjection> findCommonDelays(@Param("filter") String filter);

  @Query(
      value =
          """
          SELECT
              stu.stop_id AS stopId,
              ROUND(CAST(AVG(stu.arrival_delay) AS numeric) / 60.0, 2) AS avgDelayMinutes,
              ROUND(CAST(MAX(stu.arrival_delay) AS numeric) / 60.0, 2) AS maxDelayMinutes,
              COUNT(*) AS tripCount
          FROM external_data.bus_live_trip_updates_stop_time_updates stu
          JOIN external_data.bus_live_trip_updates ltu ON ltu.entry_id = stu.trip_update_entry_id
          JOIN external_data.bus_trips bt ON bt.id = ltu.trip_id
          WHERE bt.route_id = :routeId
            AND stu.arrival_delay IS NOT NULL
            AND stu.arrival_delay > 0
            AND (
              (:filter = 'today' AND ltu.start_date = CURRENT_DATE)
              OR (:filter = 'week'  AND ltu.start_date >= CURRENT_DATE - INTERVAL '7 days')
              OR (:filter = 'month' AND ltu.start_date >= CURRENT_DATE - INTERVAL '30 days')
            )
          GROUP BY stu.stop_id
          ORDER BY avgDelayMinutes DESC
          """,
      nativeQuery = true)
  List<BusRouteBreakdownProjection> findBreakdownByRoute(
      @Param("routeId") String routeId, @Param("filter") String filter);
}
