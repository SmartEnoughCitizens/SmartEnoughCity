package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.TramStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TramStopRepository extends JpaRepository<TramStop, String> {

  List<TramStop> findByLine(String line);

  @Query("SELECT COUNT(s) FROM TramStop s")
  long countAllStops();

  @Query("SELECT COUNT(DISTINCT s.line) FROM TramStop s")
  long countDistinctLines();

  @Query("SELECT s FROM TramStop s WHERE s.line = :line ORDER BY s.name")
  List<TramStop> findByLineOrderByName(@Param("line") String line);
}
