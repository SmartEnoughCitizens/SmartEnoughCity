package com.trinity.hermes.indicators.cycle.repository;

import com.trinity.hermes.indicators.cycle.entity.CycleOdFlowSnapshot;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleOdFlowSnapshotRepository extends JpaRepository<CycleOdFlowSnapshot, Long> {

  boolean existsBySnapshotDate(LocalDate snapshotDate);

  List<CycleOdFlowSnapshot> findBySnapshotDateOrderByEstimatedTripsDesc(LocalDate snapshotDate);
}
