package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusCommonDelayMV;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusCommonDelayMvRepository extends JpaRepository<BusCommonDelayMV, Long> {
  List<BusCommonDelayMV> findByPeriodOrderByAvgDelayMinutesDesc(String period);
}
