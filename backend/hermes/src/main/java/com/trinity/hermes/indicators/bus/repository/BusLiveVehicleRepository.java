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
          "SELECT DISTINCT ON (vehicle_id) *"
              + " FROM external_data.bus_live_vehicles"
              + " ORDER BY vehicle_id, timestamp DESC",
      nativeQuery = true)
  List<BusLiveVehicle> findLatestPositionPerVehicle();

  @Query(
      value =
          "SELECT DISTINCT ON (vehicle_id) *"
              + " FROM external_data.bus_live_vehicles"
              + " WHERE timestamp >= NOW() - INTERVAL '5 minutes'"
              + " ORDER BY vehicle_id, timestamp DESC",
      nativeQuery = true)
  List<BusLiveVehicle> findRecentVehicles();

  @Query(
      value = "SELECT COUNT(DISTINCT vehicle_id) FROM external_data.bus_live_vehicles",
      nativeQuery = true)
  Long countActiveVehicles();

  @Query(
      value =
          "SELECT DISTINCT ON (blv.vehicle_id) blv.*"
              + " FROM external_data.bus_live_vehicles blv"
              + " INNER JOIN external_data.bus_trips bt ON blv.trip_id = bt.id"
              + " WHERE bt.route_id = :routeId"
              + " ORDER BY blv.vehicle_id, blv.timestamp DESC",
      nativeQuery = true)
  List<BusLiveVehicle> findLatestPositionsByRouteId(@Param("routeId") String routeId);
}
