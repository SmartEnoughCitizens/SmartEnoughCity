package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.VTramAlternateStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VTramAlternateStopRepository extends JpaRepository<VTramAlternateStop, String> {
    List<VTramAlternateStop> findByTramStopIdOrderByDistanceM(String tramStopId);
}