package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusTrip;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusTripRepository extends JpaRepository<BusTrip, String> {

  List<BusTrip> findByRouteId(String routeId);

  @Query("SELECT COUNT(DISTINCT t.id) FROM BusTrip t WHERE t.serviceId = :serviceId")
  Long countByServiceId(@Param("serviceId") Integer serviceId);
}
