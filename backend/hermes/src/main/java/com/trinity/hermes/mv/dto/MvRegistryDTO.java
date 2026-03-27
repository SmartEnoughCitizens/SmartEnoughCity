package com.trinity.hermes.mv.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MvRegistryDTO {

  private Long id;
  private String name;
  private String description;
  private String viewSchema;

  /** The full SELECT body stored for this MV. */
  private String querySql;

  private String uniqueKeyColumns;
  private String refreshCron;
  private boolean enabled;
  private Integer version;

  private Instant createdAt;
  private Instant updatedAt;

  private Instant lastRefreshedAt;
  private Long lastRefreshDurationMs;
  private String lastRefreshStatus;
  private String lastRefreshError;

  /** Last N refresh attempts for this MV (newest first). */
  private List<MvRefreshResult> refreshHistory;
}
