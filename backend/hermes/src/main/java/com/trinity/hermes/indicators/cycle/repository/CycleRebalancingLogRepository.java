package com.trinity.hermes.indicators.cycle.repository;

import com.trinity.hermes.indicators.cycle.entity.CycleRebalancingLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleRebalancingLogRepository extends JpaRepository<CycleRebalancingLog, Long> {

  List<CycleRebalancingLog> findByLoggedAtBetweenOrderByLoggedAtDesc(Instant from, Instant to);
}
