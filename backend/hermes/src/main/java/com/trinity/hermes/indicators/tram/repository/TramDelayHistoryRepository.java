package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.TramDelayHistoryEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TramDelayHistoryRepository
    extends JpaRepository<TramDelayHistoryEntry, Integer> {

  @Query(
      value =
          "SELECT stop_id, stop_name, line,"
              + " ROUND(AVG(delay_mins)::numeric, 1) AS avg_delay,"
              + " MAX(delay_mins) AS max_delay,"
              + " COUNT(*) AS delay_count"
              + " FROM external_data.tram_delay_history"
              + " GROUP BY stop_id, stop_name, line"
              + " ORDER BY avg_delay DESC",
      nativeQuery = true)
  List<Object[]> findAvgDelayPerStop();
}
