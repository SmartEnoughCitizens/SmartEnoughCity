package com.trinity.hermes.indicators.train.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrainUtilisationHeadsignDTO {
  private String headsign;
  private String status;

  @JsonProperty("current_count")
  private double currentCount;

  @JsonProperty("predicted_count")
  private double predictedCount;

  private String recommendation;
  private List<TrainUtilisationStationDTO> stations;
}
