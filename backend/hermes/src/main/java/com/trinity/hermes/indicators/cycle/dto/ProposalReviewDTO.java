package com.trinity.hermes.indicators.cycle.dto;

import lombok.Data;

/** Payload sent by a reviewer to accept or reject a station proposal. */
@Data
public class ProposalReviewDTO {

  /** Either "ACCEPTED", "REJECTED", or "FORWARD". */
  private String action;

  /** Mandatory reason when rejecting; optional otherwise. */
  private String reason;

  /** Username of the reviewer, sourced from the frontend localStorage. */
  private String reviewedBy;
}
