package com.trinity.hermes.indicators.car.repository;

import com.trinity.hermes.indicators.car.entity.HighTrafficPoints;
import com.trinity.hermes.indicators.car.entity.HighTrafficPointsId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HighTrafficPointsRepository
    extends JpaRepository<HighTrafficPoints, HighTrafficPointsId> {

  @Query(
      value =
          "SELECT site_id, lat, lon, day_type, time_slot, avg_volume"
              + " FROM backend.traffic_aggregated",
      nativeQuery = true)
  List<Object[]> findAggregatedTrafficWithLocation();

  /**
   * Returns one row per site: [site_id, lat, lon, max_avg_volume]. Used by disruption detection to
   * identify persistently congested locations.
   */
  @Query(
      value =
          "SELECT site_id, lat, lon, MAX(avg_volume) AS max_volume"
              + " FROM backend.traffic_aggregated"
              + " WHERE lat IS NOT NULL AND lon IS NOT NULL"
              + " GROUP BY site_id, lat, lon"
              + " ORDER BY max_volume DESC",
      nativeQuery = true)
  List<Object[]> findPeakTrafficSitesWithLocation();
}
