package com.trinity.hermes.indicators.train.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrainSimulationDTO {
  private List<TrainSimResultDTO> results;

  @JsonProperty("overall_summary")
  private String overallSummary;
}
