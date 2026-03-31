package com.trinity.hermes.indicators.tram.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Read-only view of the external_data.tram_disruptions table written by the Python data_handler
 * disruption_service. Hermes polls this to bridge tram disruptions into the central disruptions
 * table.
 */
@Entity
@Table(name = "tram_disruptions", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramDisruptionExternal {

  @Id
  @Column(name = "id")
  private Integer id;

  @Column(name = "stop_id", nullable = false)
  private String stopId;

  @Column(name = "line", nullable = false)
  private String line;

  @Column(name = "message", nullable = false)
  private String message;

  @Column(name = "detected_at", nullable = false)
  private LocalDateTime detectedAt;

  @Column(name = "resolved", nullable = false)
  private Boolean resolved;
}
