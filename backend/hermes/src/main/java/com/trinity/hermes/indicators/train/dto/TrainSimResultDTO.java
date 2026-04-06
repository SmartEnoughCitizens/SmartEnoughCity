package com.trinity.hermes.indicators.train.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrainSimResultDTO {
  private String headsign;

  @JsonProperty("new_utilisation_pct")
  private double newUtilisationPct;

  @JsonProperty("passengers_absorbed")
  private int passengersAbsorbed;

  @JsonProperty("co2_saved_kg")
  private double co2SavedKg;

  @JsonProperty("sustainability_score")
  private int sustainabilityScore;

  private String summary;
}
