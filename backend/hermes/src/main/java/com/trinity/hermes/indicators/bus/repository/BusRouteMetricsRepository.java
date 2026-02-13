package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusRouteMetrics;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusRouteMetricsRepository extends JpaRepository<BusRouteMetrics, Long> {

  Optional<BusRouteMetrics> findByRouteId(String routeId);
}
