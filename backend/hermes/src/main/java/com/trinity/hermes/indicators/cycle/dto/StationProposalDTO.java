package com.trinity.hermes.indicators.cycle.dto;

import java.util.List;
import lombok.Data;

/** Payload for a simulated station placement proposal submitted by a user. */
@Data
public class StationProposalDTO {

  /** Proposed station coordinates. */
  @Data
  public static class ProposedStation {
    private double latitude;
    private double longitude;
  }

  /** Areas whose coverage would improve as a result of the proposed stations. */
  @Data
  public static class ImpactedArea {
    private String electoralDivision;
    private String fromCategory;
    private String toCategory;
    private double simulatedDistanceM;
  }

  /** The proposed station locations. */
  private List<ProposedStation> proposedStations;

  /** Coverage impact per affected electoral division. */
  private List<ImpactedArea> impactedAreas;

  /** Total number of areas that would improve. */
  private int totalImprovedAreas;

  /** Username of the person who submitted the proposal. */
  private String submittedBy;

  /** Free-text notes from the submitter (optional). */
  private String notes;
}
