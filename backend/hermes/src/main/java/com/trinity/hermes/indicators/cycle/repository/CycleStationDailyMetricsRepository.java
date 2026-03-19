package com.trinity.hermes.indicators.cycle.repository;

import com.trinity.hermes.indicators.cycle.entity.CycleStationDailyMetrics;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleStationDailyMetricsRepository
    extends JpaRepository<CycleStationDailyMetrics, Long> {

  Optional<CycleStationDailyMetrics> findByStationIdAndMetricDate(
      Integer stationId, LocalDate metricDate);

  boolean existsByStationIdAndMetricDate(Integer stationId, LocalDate metricDate);
}
