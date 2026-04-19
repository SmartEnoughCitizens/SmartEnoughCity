package com.trinity.hermes.approval.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateApprovalRequestDTO {
  /** Indicator tag: "train", "bus", "cycle", etc. */
  private String indicator;

  /** Arbitrary JSON payload (corridors, recommendations, etc.). */
  private String payloadJson;

  /** Short human-readable summary shown to the reviewer. */
  private String summary;

  /** Optional deep-link included in approval notifications (e.g. "/train?tab=approvals"). */
  private String actionUrl;

  /**
   * Optional ID of the source recommendation row. When set, only that specific row is marked
   * submitted instead of all pending rows for the indicator.
   */
  private Integer recommendationId;
}
