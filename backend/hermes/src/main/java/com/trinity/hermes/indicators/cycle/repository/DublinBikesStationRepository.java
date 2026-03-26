package com.trinity.hermes.indicators.cycle.repository;

import com.trinity.hermes.indicators.cycle.entity.DublinBikesStation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DublinBikesStationRepository extends JpaRepository<DublinBikesStation, Integer> {

  List<DublinBikesStation> findByRegionId(String regionId);

  @Query(
      value =
          """
          SELECT st.station_id, st.created_at::date AS added_date, st.capacity
          FROM external_data.dublin_bikes_stations st
          ORDER BY st.created_at
          """,
      nativeQuery = true)
  List<Object[]> findFleetTimeline();
}
