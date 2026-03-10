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
          "SELECT tv.site_id, tv.end_time, SUM(tv.sum_volume) AS total_volume, ss.lat, ss.lon"
              + " FROM external_data.traffic_volumes tv"
              + " LEFT JOIN external_data.scats_sites ss ON tv.site_id = ss.site_id"
              + " GROUP BY tv.site_id, tv.end_time, ss.lat, ss.lon",
      nativeQuery = true)
  List<Object[]> findAggregatedTrafficWithLocation();
}
