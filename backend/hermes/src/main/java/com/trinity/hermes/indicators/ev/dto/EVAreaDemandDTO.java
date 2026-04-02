package com.trinity.hermes.indicators.ev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EVAreaDemandDTO {

  private String area;

  @JsonProperty("registered_evs")
  private Integer registeredEvs;

  @JsonProperty("charging_demand")
  private Integer chargingDemand;

  @JsonProperty("home_charge_percentage")
  private Double homeChargePercentage;

  @JsonProperty("charge_frequency")
  private Double chargeFrequency;
}
