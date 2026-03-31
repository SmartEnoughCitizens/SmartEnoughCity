package com.trinity.hermes.indicators.ev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EVChargingStationsResponseDTO {

  @JsonProperty("total_stations")
  private Integer totalStations;

  private List<EVStationDTO> stations;
}
