package com.trinity.hermes.mv.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MvRefreshResult {

  private String mvName;

  /** SUCCESS, FAILED, or SKIPPED (when a refresh is already in progress). */
  private String status;

  private Long durationMs;
  private Instant refreshedAt;
  private String errorMessage;
}
