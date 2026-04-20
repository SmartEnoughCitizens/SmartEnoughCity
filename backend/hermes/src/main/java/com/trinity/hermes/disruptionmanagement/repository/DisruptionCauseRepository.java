package com.trinity.hermes.disruptionmanagement.repository;

import com.trinity.hermes.disruptionmanagement.entity.DisruptionCause;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisruptionCauseRepository extends JpaRepository<DisruptionCause, Long> {
  List<DisruptionCause> findByDisruptionId(Long disruptionId);

  void deleteByDisruptionId(Long disruptionId);
}
