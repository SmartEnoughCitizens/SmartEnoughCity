package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusLiveVehicle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusLiveVehicleRepository extends JpaRepository<BusLiveVehicle, Integer> {

  @Query(
      value =
          "SELECT blv.* FROM external_data.bus_live_vehicles blv"
              + " INNER JOIN (SELECT vehicle_id, MAX(timestamp) as max_ts"
              + " FROM external_data.bus_live_vehicles GROUP BY vehicle_id) latest"
              + " ON blv.vehicle_id = latest.vehicle_id AND blv.timestamp = latest.max_ts",
      nativeQuery = true)
  List<BusLiveVehicle> findLatestPositionPerVehicle();

  @Query(
      value =
          "SELECT COUNT(DISTINCT vehicle_id) FROM external_data.bus_live_vehicles"
              + " WHERE timestamp > NOW() - INTERVAL '30 minutes'",
      nativeQuery = true)
  Long countActiveVehicles();

  @Query(
      value =
          "SELECT blv.* FROM external_data.bus_live_vehicles blv"
              + " INNER JOIN external_data.bus_trips bt ON blv.trip_id = bt.id"
              + " INNER JOIN (SELECT vehicle_id, MAX(timestamp) as max_ts"
              + " FROM external_data.bus_live_vehicles GROUP BY vehicle_id) latest"
              + " ON blv.vehicle_id = latest.vehicle_id AND blv.timestamp = latest.max_ts"
              + " WHERE bt.route_id = :routeId",
      nativeQuery = true)
  List<BusLiveVehicle> findLatestPositionsByRouteId(@Param("routeId") String routeId);
}
