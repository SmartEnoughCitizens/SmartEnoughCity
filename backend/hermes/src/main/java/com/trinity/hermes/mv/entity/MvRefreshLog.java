package com.trinity.hermes.mv.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mv_refresh_log", schema = "backend")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MvRefreshLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "mv_name", nullable = false, length = 100)
  private String mvName;

  /** SUCCESS or FAILED */
  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(name = "refreshed_at", nullable = false)
  private Instant refreshedAt;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  /** SCHEDULER, API, or REFRESH_ALL */
  @Column(name = "triggered_by", length = 50)
  private String triggeredBy;
}
