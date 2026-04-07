package com.trinity.hermes.disruptionmanagement.repository;

import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  /** Find disruptions detected after a given time (for deduplication checks) */
  List<Disruption> findByDisruptionTypeAndAffectedAreaAndDetectedAtAfter(
      String disruptionType, String affectedArea, LocalDateTime after);

  /** Check whether an ACTIVE disruption already exists for the given type and area */
  boolean existsByDisruptionTypeAndAffectedAreaAndStatus(
      String disruptionType, String affectedArea, String status);

  /** Find the most recent non-resolved disruptions ordered by detected time */
  @Query(
      "SELECT d FROM Disruption d WHERE d.status NOT IN ('RESOLVED', 'CANCELLED')"
          + " ORDER BY d.detectedAt DESC")
  List<Disruption> findAllActiveOrderByDetectedAtDesc(Pageable pageable);

  /** Convenience overload — returns top 200 */
  default List<Disruption> findAllActiveOrderByDetectedAtDesc() {
    return findAllActiveOrderByDetectedAtDesc(PageRequest.of(0, 200));
  }

  /** Find ACTIVE disruptions whose estimatedEndTime is in the past (for auto-expiry) */
  @Query(
      "SELECT d FROM Disruption d WHERE d.status = 'ACTIVE'"
          + " AND d.estimatedEndTime IS NOT NULL"
          + " AND d.estimatedEndTime < :now")
  List<Disruption> findExpiredActiveDisruptions(@Param("now") LocalDateTime now);
}
