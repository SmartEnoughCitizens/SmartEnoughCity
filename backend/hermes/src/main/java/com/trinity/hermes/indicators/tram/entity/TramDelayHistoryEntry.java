package com.trinity.hermes.indicators.tram.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "tram_delay_history", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramDelayHistoryEntry {

  @Id
  @Column(name = "id")
  private Integer id;

  @Column(name = "recorded_at")
  private OffsetDateTime recordedAt;

  @Column(name = "stop_id", nullable = false)
  private String stopId;

  @Column(name = "stop_name", nullable = false)
  private String stopName;

  @Column(name = "line", nullable = false)
  private String line;

  @Column(name = "direction", nullable = false)
  private String direction;

  @Column(name = "destination", nullable = false)
  private String destination;

  @Column(name = "scheduled_time")
  private String scheduledTime;

  @Column(name = "due_mins", nullable = false)
  private Integer dueMins;

  @Column(name = "delay_mins", nullable = false)
  private Integer delayMins;

  @Column(name = "estimated_affected_passengers")
  private Double estimatedAffectedPassengers;
}
