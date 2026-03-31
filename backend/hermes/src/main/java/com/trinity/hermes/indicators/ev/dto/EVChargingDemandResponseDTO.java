package com.trinity.hermes.indicators.ev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EVChargingDemandResponseDTO {

  private Map<String, Object> summary;

  @JsonProperty("high_priority_areas")
  private List<String> highPriorityAreas;

  private List<EVAreaDemandDTO> areas;
}
