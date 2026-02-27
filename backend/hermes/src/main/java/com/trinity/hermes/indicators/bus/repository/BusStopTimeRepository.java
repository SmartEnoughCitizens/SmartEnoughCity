package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusStopTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusStopTimeRepository extends JpaRepository<BusStopTime, Integer> {

  List<BusStopTime> findByTripId(String tripId);

  List<BusStopTime> findByStopId(String stopId);

  @Query(
      value =
          "SELECT trip_id FROM ("
              + " SELECT trip_id, MIN(departure_time) AS first_dep, MAX(arrival_time) AS last_arr"
              + " FROM external_data.bus_stop_times"
              + " GROUP BY trip_id"
              + ") t WHERE :currentTime BETWEEN first_dep AND last_arr",
      nativeQuery = true)
  List<String> findAllActiveTripsAtTime(@Param("currentTime") java.sql.Time currentTime);
}
