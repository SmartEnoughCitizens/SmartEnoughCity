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

  @Query(
      value =
          "SELECT st.* FROM external_data.tram_stop_times st"
              + " WHERE st.stop_id IN :stopIds"
              + " AND st.arrival_time >= :from"
              + " AND st.arrival_time <= :to"
              + " ORDER BY st.arrival_time",
      nativeQuery = true)
  List<TramStopTime> findByStopIdInAndArrivalTimeBetween(
      @Param("stopIds") List<String> stopIds,
      @Param("from") java.sql.Time from,
      @Param("to") java.sql.Time to);
}
