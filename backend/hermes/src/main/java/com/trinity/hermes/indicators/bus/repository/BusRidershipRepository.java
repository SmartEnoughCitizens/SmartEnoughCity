package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusRidership;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusRidershipRepository extends JpaRepository<BusRidership, Integer> {

  @Query(
      value =
          "SELECT * FROM external_data.bus_ridership"
              + " WHERE vehicle_id = :vehicleId ORDER BY timestamp DESC LIMIT 1",
      nativeQuery = true)
  BusRidership findLatestByVehicleId(@Param("vehicleId") Integer vehicleId);

  @Query(
      value =
          "SELECT DISTINCT ON (vehicle_id) * FROM external_data.bus_ridership"
              + " ORDER BY vehicle_id, timestamp DESC",
      nativeQuery = true)
  List<BusRidership> findLatestPerVehicle();

  @Query(
      value =
          "SELECT AVG(passengers_onboard::float / NULLIF(vehicle_capacity, 0))"
              + " FROM external_data.bus_ridership r"
              + " INNER JOIN external_data.bus_trips t ON r.trip_id = t.id"
              + " WHERE t.route_id = :routeId",
      nativeQuery = true)
  Double findAverageOccupancyByRouteId(@Param("routeId") String routeId);
}
