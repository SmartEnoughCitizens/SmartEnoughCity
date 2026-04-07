package com.trinity.hermes.indicators.cycle.dto;

import lombok.Data;

/** Payload sent by a reviewer to accept or reject a station proposal. */
@Data
public class ProposalReviewDTO {

  /** Either "ACCEPTED" or "REJECTED". */
  private String action;

  /** Mandatory reason when rejecting; optional when accepting. */
  private String reason;
}
