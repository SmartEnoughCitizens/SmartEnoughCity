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

  @Query(
      value =
          "SELECT stu.* FROM external_data.bus_live_trip_updates_stop_time_updates stu"
              + " INNER JOIN external_data.bus_live_trip_updates tu"
              + " ON stu.trip_update_entry_id = tu.entry_id",
      nativeQuery = true)
  List<BusLiveStopTimeUpdate> findRecentStopTimeUpdates();

  // Returns [vehicle_id, arrival_delay, departure_delay] — used to index delays by vehicle_id
  @Query(
      value =
          "SELECT tu.vehicle_id, stu.arrival_delay, stu.departure_delay"
              + " FROM external_data.bus_live_trip_updates_stop_time_updates stu"
              + " INNER JOIN external_data.bus_live_trip_updates tu"
              + " ON stu.trip_update_entry_id = tu.entry_id",
      nativeQuery = true)
  List<Object[]> findDelaysByVehicle();
}
