package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.TramStopTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TramStopTimeRepository extends JpaRepository<TramStopTime, Integer> {

  List<TramStopTime> findByTripId(String tripId);

  List<TramStopTime> findByStopId(String stopId);

  @Query(
      value =
          "SELECT trip_id FROM ("
              + " SELECT trip_id, MIN(departure_time) AS first_dep, MAX(arrival_time) AS last_arr"
              + " FROM external_data.tram_stop_times"
              + " GROUP BY trip_id"
              + ") t WHERE :currentTime BETWEEN first_dep AND last_arr",
      nativeQuery = true)
  List<String> findActiveTripsAtTime(@Param("currentTime") java.sql.Time currentTime);

  @Query(
      value =
          "SELECT st.* FROM external_data.tram_stop_times st"
              + " WHERE st.stop_id = :stopId"
              + " ORDER BY st.arrival_time",
      nativeQuery = true)
  List<TramStopTime> findByStopIdOrderByArrivalTime(@Param("stopId") String stopId);

  /**
   * Calendar-weighted average daily trip count per stop.
   *
   * <p>Mirrors the train demand pipeline: groups stop_times by (stop, service_id), counts distinct
   * trips, weights by days-per-week the service runs, then divides by 7 to get a daily average.
   * This avoids the raw row-count inflation caused by multiple service patterns in the GTFS feed.
   *
   * <p>Returns Object[] rows of [stop_id (String), daily_trip_count (Integer)].
   */
  @Query(
      value =
          "SELECT sub.stop_id,"
              + "  GREATEST(1, ROUND(SUM(sub.trip_count * ("
              + "    (cs.monday::int) + (cs.tuesday::int) + (cs.wednesday::int) +"
              + "    (cs.thursday::int) + (cs.friday::int) +"
              + "    (cs.saturday::int) + (cs.sunday::int)"
              + "  )) / 7.0)::int) AS daily_trip_count"
              + " FROM ("
              + "   SELECT st.stop_id, t.service_id, COUNT(DISTINCT st.trip_id) AS trip_count"
              + "   FROM external_data.tram_stop_times st"
              + "   JOIN external_data.tram_trips t ON t.id = st.trip_id"
              + "   GROUP BY st.stop_id, t.service_id"
              + " ) sub"
              + " JOIN external_data.tram_calendar_schedule cs ON cs.service_id = sub.service_id"
              + " GROUP BY sub.stop_id",
      nativeQuery = true)
  List<Object[]> findDailyTripCountsPerStop();
}
