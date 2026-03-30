package com.trinity.hermes.mv.repository;

import com.trinity.hermes.mv.entity.MvRegistry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MvRegistryRepository extends JpaRepository<MvRegistry, Long> {

  Optional<MvRegistry> findByName(String name);

  List<MvRegistry> findAllByEnabledTrue();

  boolean existsByName(String name);
}
