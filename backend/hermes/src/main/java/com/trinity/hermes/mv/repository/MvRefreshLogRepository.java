package com.trinity.hermes.mv.repository;

import com.trinity.hermes.mv.entity.MvRefreshLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MvRefreshLogRepository extends JpaRepository<MvRefreshLog, Long> {

  List<MvRefreshLog> findByMvNameOrderByRefreshedAtDesc(String mvName);

  /** Deletes all rows for a given MV beyond the most recent N, keeping the log table small. */
  @Modifying
  @Query(value = """
      DELETE FROM backend.mv_refresh_log
      WHERE mv_name = :mvName
        AND id NOT IN (
          SELECT id FROM backend.mv_refresh_log
          WHERE mv_name = :mvName
          ORDER BY refreshed_at DESC
          LIMIT :keepCount
        )
      """, nativeQuery = true)
  void pruneOldLogs(@Param("mvName") String mvName, @Param("keepCount") int keepCount);

  /** Remove all log rows when a MV is dropped. */
  void deleteByMvName(String mvName);
}
