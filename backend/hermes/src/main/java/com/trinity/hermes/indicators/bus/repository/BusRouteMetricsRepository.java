package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusRouteMetrics;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BusRouteMetricsRepository extends JpaRepository<BusRouteMetrics, Long> {

  Optional<BusRouteMetrics> findByRouteId(String routeId);

  @Query("SELECT AVG(m.utilizationPct) FROM BusRouteMetrics m WHERE m.utilizationPct IS NOT NULL")
  Double findAverageUtilization();

  @Query("SELECT AVG(m.reliabilityPct) FROM BusRouteMetrics m WHERE m.reliabilityPct IS NOT NULL")
  Double findAverageReliability();

  @Query("SELECT AVG(m.lateArrivalPct) FROM BusRouteMetrics m WHERE m.lateArrivalPct IS NOT NULL")
  Double findAverageLateArrival();

  void deleteByRouteId(String routeId);
}
