package com.trinity.hermes.indicators.train.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/** One corridor entry inside a demand-simulation request. */
@Data
@NoArgsConstructor
public class TrainDemandCorridorDTO {
  private String originStopId;
  private String destinationStopId;

  /** Number of new trains to add on this corridor (1–20). */
  private int trainCount = 1;
}
