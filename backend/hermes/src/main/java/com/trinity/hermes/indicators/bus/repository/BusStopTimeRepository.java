package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusStopTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusStopTimeRepository extends JpaRepository<BusStopTime, Integer> {

  List<BusStopTime> findByTripId(String tripId);

  List<BusStopTime> findByStopId(String stopId);
}
