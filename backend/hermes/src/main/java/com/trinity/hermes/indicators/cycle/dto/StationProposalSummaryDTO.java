package com.trinity.hermes.indicators.cycle.dto;

import lombok.Data;

/** Summary of a submitted station proposal returned to reviewers. */
@Data
public class StationProposalSummaryDTO {

  private Long id;
  private String submittedAt;
  private String submittedBy;
  private String submittedByRole;
  private int stationCount;
  private int improvedAreaCount;
  private String status;
  private String notes;

  /** Raw JSON array of proposed station coordinates. Format: [{"lat":53.123,"lon":-6.456},...] */
  private String stationsJson;

  /**
   * Raw JSON array of coverage impact per area. Format:
   * [{"area":"...","from":"...","to":"...","distM":123.4},...]
   */
  private String impactsJson;
}
