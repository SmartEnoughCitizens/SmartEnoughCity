package com.trinity.hermes.indicators.train.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response for a demand simulation: base vs. simulated demand per station. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrainDemandSimulateResponseDTO {
  @JsonAlias("base_demand")
  private List<TrainStationDemandDTO> baseDemand;

  @JsonAlias("simulated_demand")
  private List<TrainStationDemandDTO> simulatedDemand;

  @JsonAlias("affected_stop_ids")
  private List<String> affectedStopIds;
}
