package com.trinity.hermes.config;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficAggregatedViewInitializer {

  private static final String CREATE_VIEW_SQL =
      """
      CREATE OR REPLACE VIEW backend.traffic_aggregated AS
      SELECT
          tv.site_id,
          s.lat,
          s.lon,
          CASE
              WHEN EXTRACT(DOW FROM tv.end_time) IN (0, 6) THEN 'weekend'
              ELSE 'weekday'
          END AS day_type,
          CASE
              WHEN EXTRACT(HOUR FROM tv.end_time) BETWEEN 7 AND 9  THEN 'morning_peak'
              WHEN EXTRACT(HOUR FROM tv.end_time) BETWEEN 10 AND 15 THEN 'inter_peak'
              WHEN EXTRACT(HOUR FROM tv.end_time) BETWEEN 16 AND 18 THEN 'evening_peak'
              ELSE 'off_peak'
          END AS time_slot,
          AVG(tv.avg_volume) AS avg_volume
      FROM external_data.traffic_volumes tv
      JOIN external_data.scats_sites s ON tv.site_id = s.site_id
      GROUP BY
          tv.site_id,
          s.lat,
          s.lon,
          CASE WHEN EXTRACT(DOW FROM tv.end_time) IN (0, 6) THEN 'weekend' ELSE 'weekday' END,
          CASE
              WHEN EXTRACT(HOUR FROM tv.end_time) BETWEEN 7 AND 9  THEN 'morning_peak'
              WHEN EXTRACT(HOUR FROM tv.end_time) BETWEEN 10 AND 15 THEN 'inter_peak'
              WHEN EXTRACT(HOUR FROM tv.end_time) BETWEEN 16 AND 18 THEN 'evening_peak'
              ELSE 'off_peak'
          END
      """;

  private final DataSource dataSource;

  @EventListener(ApplicationReadyEvent.class)
  public void createTrafficAggregatedView() {
    log.info("Creating backend.traffic_aggregated view...");
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      conn.setAutoCommit(true);
      stmt.execute(CREATE_VIEW_SQL);
      log.info("backend.traffic_aggregated view ready.");
    } catch (Exception e) {
      log.warn(
          "Could not create backend.traffic_aggregated view (source tables may not exist yet): {}",
          e.getMessage());
    }
  }
}
