package com.trinity.hermes.mv.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mv_registry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MvRegistry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, unique = true, length = 100)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "view_schema", nullable = false, length = 50)
  private String viewSchema;

  /** The SELECT body of the MV — stored exactly as provided via the API. */
  @Column(name = "query_sql", nullable = false, columnDefinition = "TEXT")
  private String querySql;

  /** Comma-separated column names used to create UNIQUE indexes (required for REFRESH CONCURRENTLY). */
  @Column(name = "unique_key_columns", nullable = false, columnDefinition = "TEXT")
  private String uniqueKeyColumns;

  /** Spring 6-field cron expression. Null means no scheduled auto-refresh. */
  @Column(name = "refresh_cron", length = 100)
  private String refreshCron;

  @Column(name = "last_refreshed_at")
  private Instant lastRefreshedAt;

  @Column(name = "last_refresh_duration_ms")
  private Long lastRefreshDurationMs;

  /** SUCCESS, FAILED, or SKIPPED */
  @Column(name = "last_refresh_status", length = 20)
  private String lastRefreshStatus;

  @Column(name = "last_refresh_error", columnDefinition = "TEXT")
  private String lastRefreshError;

  /** Incremented on every upsert so definition history is traceable. */
  @Column(name = "version", nullable = false)
  @Builder.Default
  private Integer version = 1;

  @Column(name = "enabled", nullable = false)
  @Builder.Default
  private boolean enabled = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
