package com.trinity.hermes.indicators.pedestrians.repository;

import com.trinity.hermes.indicators.pedestrians.entity.PedestrianSite;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PedestrianSiteRepository extends JpaRepository<PedestrianSite, Integer> {

  /**
   * Returns the total pedestrian count per site at its most recent measurement time. Only includes
   * sites that have at least one measure and have a pedestrian sensor.
   */
  @Query(
      value =
          """
          SELECT
              pcs.id          AS site_id,
              pcs.name        AS site_name,
              pcs.lat         AS lat,
              pcs.lon         AS lon,
              SUM(pcm.count)  AS total_count,
              MAX(pcm.end_datetime) AS last_updated
          FROM external_data.pedestrian_counter_sites pcs
          JOIN external_data.pedestrian_channels pc
               ON pc.site_id = pcs.id
          JOIN external_data.pedestrian_counter_measures pcm
               ON pcm.channel_id = pc.channel_id
          INNER JOIN (
              SELECT pc2.site_id, MAX(pcm2.start_datetime) AS max_dt
              FROM external_data.pedestrian_counter_measures pcm2
              JOIN external_data.pedestrian_channels pc2
                   ON pc2.channel_id = pcm2.channel_id
              GROUP BY pc2.site_id
          ) latest ON pc.site_id = latest.site_id
                   AND pcm.start_datetime = latest.max_dt
          WHERE pcs.pedestrian_sensor = true
          GROUP BY pcs.id, pcs.name, pcs.lat, pcs.lon
          ORDER BY total_count DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<Object[]> findLatestPedestrianCountsPerSite(@Param("limit") int limit);
}
