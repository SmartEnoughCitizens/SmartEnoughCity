package com.trinity.hermes.indicators.train.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrainUtilisationStationDTO {
  private String name;
  private double lat;
  private double lon;

  @JsonProperty("utilisation_ratio")
  private double utilisationRatio;
}
