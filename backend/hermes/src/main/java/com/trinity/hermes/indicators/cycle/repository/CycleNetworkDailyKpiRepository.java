package com.trinity.hermes.indicators.cycle.repository;

import com.trinity.hermes.indicators.cycle.entity.CycleNetworkDailyKpi;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleNetworkDailyKpiRepository extends JpaRepository<CycleNetworkDailyKpi, Long> {

  Optional<CycleNetworkDailyKpi> findByMetricDate(LocalDate metricDate);
}
