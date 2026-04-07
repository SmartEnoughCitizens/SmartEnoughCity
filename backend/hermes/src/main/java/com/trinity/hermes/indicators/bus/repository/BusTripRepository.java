package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusTrip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusTripRepository extends JpaRepository<BusTrip, String> {

  List<BusTrip> findByRouteId(String routeId);

  /**
   * First trip for a route by lexicographic trip id — used to pick a canonical shape for the route.
   */
  Optional<BusTrip> findFirstByRouteIdOrderByIdAsc(String routeId);

  @Query("SELECT COUNT(DISTINCT t.id) FROM BusTrip t WHERE t.serviceId = :serviceId")
  Long countByServiceId(@Param("serviceId") Integer serviceId);
}
