package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.TramRoute;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TramRouteRepository extends JpaRepository<TramRoute, String> {

  @Query("SELECT r FROM TramRoute r ORDER BY r.shortName")
  List<TramRoute> findAllOrderByShortName();
}
