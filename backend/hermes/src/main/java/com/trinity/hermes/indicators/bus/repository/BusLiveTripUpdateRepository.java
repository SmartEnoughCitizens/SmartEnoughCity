package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusLiveTripUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BusLiveTripUpdateRepository extends JpaRepository<BusLiveTripUpdate, Integer> {

  @Query(
      value =
          "SELECT COUNT(DISTINCT vehicle_id) FROM external_data.bus_live_trip_updates"
              + " WHERE timestamp > NOW() - INTERVAL '30 minutes'",
      nativeQuery = true)
  Long countActiveUpdates();
}
