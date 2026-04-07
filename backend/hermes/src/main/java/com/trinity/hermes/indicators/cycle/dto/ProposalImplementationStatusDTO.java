package com.trinity.hermes.indicators.cycle.dto;

import lombok.Data;

/** Request body for updating a proposal's implementation lifecycle status. */
@Data
public class ProposalImplementationStatusDTO {

  /** One of: PLANNED, IN_PROGRESS, COMPLETED */
  private String status;
}
