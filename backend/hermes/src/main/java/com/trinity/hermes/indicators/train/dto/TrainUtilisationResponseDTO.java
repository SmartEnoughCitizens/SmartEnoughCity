package com.trinity.hermes.indicators.train.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrainUtilisationResponseDTO {
  private List<TrainUtilisationHeadsignDTO> headsigns;
  private TrainSimulationDTO simulation;
}
