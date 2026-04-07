package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusLiveStopTimeUpdate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusLiveStopTimeUpdateRepository
    extends JpaRepository<BusLiveStopTimeUpdate, Integer> {

  @Query(
      value =
          "SELECT COUNT(DISTINCT tu.vehicle_id)"
              + " FROM external_data.bus_live_trip_updates_stop_time_updates stu"
              + " INNER JOIN external_data.bus_live_trip_updates tu"
              + " ON stu.trip_update_entry_id = tu.entry_id"
              + " WHERE (stu.arrival_delay > :thresholdSeconds"
              + " OR stu.departure_delay > :thresholdSeconds)",
      nativeQuery = true)
  Long countActiveDelays(@Param("thresholdSeconds") Integer thresholdSeconds);

  /**
   * For each route, find the stop with the worst current arrival delay. Returns [route_id, stop_id,
   * stop_name, lat, lon, max_arrival_delay_seconds]. Only includes stops where max_arrival_delay
   * exceeds the given threshold.
   */
  @Query(
      value =
          "SELECT bt.route_id, bs.id AS stop_id, bs.name AS stop_name,"
              + " bs.lat, bs.lon, MAX(stu.arrival_delay) AS max_delay"
              + " FROM external_data.bus_live_trip_updates_stop_time_updates stu"
              + " JOIN external_data.bus_live_trip_updates tu"
              + "   ON stu.trip_update_entry_id = tu.entry_id"
              + " JOIN external_data.bus_trips bt ON tu.trip_id = bt.id"
              + " JOIN external_data.bus_stops bs ON stu.stop_id = bs.id"
              + " WHERE stu.arrival_delay > :thresholdSeconds"
              + " GROUP BY bt.route_id, bs.id, bs.name, bs.lat, bs.lon"
              + " ORDER BY max_delay DESC",
      nativeQuery = true)
  List<Object[]> findWorstDelayedStopPerRoute(@Param("thresholdSeconds") int thresholdSeconds);

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
            AND ltu.start_date >= CASE
                WHEN :filter = 'today' THEN CURRENT_DATE
                WHEN :filter = 'week'  THEN CURRENT_DATE - INTERVAL '7 days'
                ELSE                        CURRENT_DATE - INTERVAL '30 days'
            END
            AND ltu.start_date <= CURRENT_DATE
          GROUP BY stu.stop_id
          ORDER BY avgDelayMinutes DESC
          """,
      nativeQuery = true)
  List<BusRouteBreakdownProjection> findBreakdownByRoute(
      @Param("routeId") String routeId, @Param("filter") String filter);
}
