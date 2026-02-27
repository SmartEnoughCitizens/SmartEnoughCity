package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusRoute;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BusRouteRepository extends JpaRepository<BusRoute, String> {

  @Query("SELECT r FROM BusRoute r ORDER BY r.shortName")
  List<BusRoute> findAllOrderByShortName();

  List<BusRoute> findByAgencyId(Integer agencyId);
}
