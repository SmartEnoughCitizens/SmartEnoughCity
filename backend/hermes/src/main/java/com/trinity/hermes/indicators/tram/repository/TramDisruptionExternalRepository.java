package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.TramDisruptionExternal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TramDisruptionExternalRepository
    extends JpaRepository<TramDisruptionExternal, Integer> {

  /** Unresolved disruptions detected after the given timestamp. */
  @Query(
      "SELECT d FROM TramDisruptionExternal d WHERE d.resolved = false AND d.detectedAt > :since")
  List<TramDisruptionExternal> findActiveDisruptionsSince(@Param("since") LocalDateTime since);
}
