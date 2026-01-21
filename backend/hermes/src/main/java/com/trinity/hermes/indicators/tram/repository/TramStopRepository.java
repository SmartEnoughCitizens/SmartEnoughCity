package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.TramStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TramStopRepository extends JpaRepository<TramStop, String> {
  List<TramStop> findByLine(String line);
}
