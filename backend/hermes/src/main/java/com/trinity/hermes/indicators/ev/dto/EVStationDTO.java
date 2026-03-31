package com.trinity.hermes.indicators.ev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EVStationDTO {

  private String address;
  private String county;
  private Double latitude;
  private Double longitude;

  @JsonProperty("charger_count")
  private Integer chargerCount;

  @JsonProperty("open_hours")
  private String openHours;
}
