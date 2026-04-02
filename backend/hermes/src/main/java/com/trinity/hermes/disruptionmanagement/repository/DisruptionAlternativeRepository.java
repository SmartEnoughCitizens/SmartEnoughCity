package com.trinity.hermes.disruptionmanagement.repository;

import com.trinity.hermes.disruptionmanagement.entity.DisruptionAlternative;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisruptionAlternativeRepository
    extends JpaRepository<DisruptionAlternative, Long> {
  List<DisruptionAlternative> findByDisruptionId(Long disruptionId);
}
