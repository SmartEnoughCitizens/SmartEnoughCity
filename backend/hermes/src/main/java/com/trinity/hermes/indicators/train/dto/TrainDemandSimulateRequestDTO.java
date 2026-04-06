package com.trinity.hermes.indicators.train.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for the demand-simulation endpoint. Up to 3 corridors supported. */
@Data
@NoArgsConstructor
public class TrainDemandSimulateRequestDTO {
  /** List of corridors to simulate (max 3). */
  private List<TrainDemandCorridorDTO> corridors;
}
