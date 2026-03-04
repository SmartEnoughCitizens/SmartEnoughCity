package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusStopRepository extends JpaRepository<BusStop, String> {

  @Query(
      "SELECT s FROM BusStop s WHERE s.lat BETWEEN :minLat AND :maxLat"
          + " AND s.lon BETWEEN :minLon AND :maxLon")
  List<BusStop> findStopsInBounds(
      @Param("minLat") Double minLat,
      @Param("maxLat") Double maxLat,
      @Param("minLon") Double minLon,
      @Param("maxLon") Double maxLon);
}
