package com.trinity.hermes.disruptionmanagement.repository;

import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository interface for Disruption entity with custom query methods */
@Repository
public interface DisruptionRepository extends JpaRepository<Disruption, Long> {

  /** Find disruptions by status */
  List<Disruption> findByStatus(String status);

  /** Find disruptions by severity */
  List<Disruption> findBySeverity(String severity);

  /** Find disruptions by affected area */
  List<Disruption> findByAffectedArea(String area);

  /** Find disruptions by status ordered by detected time (most recent first) */
  List<Disruption> findByStatusOrderByDetectedAtDesc(String status);
}
