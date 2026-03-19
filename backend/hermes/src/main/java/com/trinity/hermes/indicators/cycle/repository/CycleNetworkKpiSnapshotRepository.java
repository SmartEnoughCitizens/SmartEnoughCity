package com.trinity.hermes.indicators.cycle.repository;

import com.trinity.hermes.indicators.cycle.entity.CycleNetworkKpiSnapshot;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleNetworkKpiSnapshotRepository
    extends JpaRepository<CycleNetworkKpiSnapshot, Long> {

  Optional<CycleNetworkKpiSnapshot> findBySnapshotAt(Instant snapshotAt);
}
