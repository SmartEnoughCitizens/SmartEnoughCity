package com.trinity.hermes.mv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpsertMvRequest {

  /** Logical identifier for the MV. Used as the Postgres object name — alphanumeric + underscores only. */
  @NotBlank
  @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "name must be lowercase alphanumeric with underscores")
  private String name;

  private String description;

  /**
   * Schema to create the MV in. Defaults to "backend" if not provided.
   * Must be alphanumeric + underscores to prevent SQL injection.
   */
  @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "viewSchema must be lowercase alphanumeric with underscores")
  private String viewSchema;

  /**
   * The SELECT body of the materialized view — everything that follows the AS keyword
   * in CREATE MATERIALIZED VIEW. Must not contain semicolons (blocks statement chaining).
   */
  @NotBlank
  private String querySql;

  //Column names to create UNIQUE indexes on. At least one is required to
  @NotBlank
  private String uniqueKeyColumns;

  /**
   * Spring 6-field cron expression for scheduled refresh, e.g. "0 0/5 * * * *".
   * Null or blank means no scheduled refresh.
   */
  private String refreshCron;

  /** If false, the MV is registered but skipped by the scheduler and refresh-all. Defaults to true. */
  private Boolean enabled;
}
