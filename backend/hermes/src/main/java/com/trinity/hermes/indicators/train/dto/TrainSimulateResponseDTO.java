package com.trinity.hermes.indicators.train.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrainSimulateResponseDTO {
  private TrainSimulationDTO simulation;

  @JsonProperty("already_simulated")
  private boolean alreadySimulated;
}
