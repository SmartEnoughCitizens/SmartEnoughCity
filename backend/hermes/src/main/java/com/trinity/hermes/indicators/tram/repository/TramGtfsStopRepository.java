package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.TramGtfsStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TramGtfsStopRepository extends JpaRepository<TramGtfsStop, String> {

  @Query("SELECT s FROM TramGtfsStop s WHERE LOWER(s.name) = LOWER(:name)")
  List<TramGtfsStop> findByNameIgnoreCase(@Param("name") String name);
}
