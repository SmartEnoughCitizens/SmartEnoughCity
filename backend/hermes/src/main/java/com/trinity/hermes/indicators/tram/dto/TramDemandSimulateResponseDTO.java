package com.trinity.hermes.indicators.tram.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response body for POST /api/v1/tram/stop-demand/simulate. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TramDemandSimulateResponseDTO {

  /** Demand scores before adding extra trams. */
  private List<TramStopDemandDTO> baseDemand;

  /** Demand scores after adding extra trams. */
  private List<TramStopDemandDTO> simulatedDemand;

  /** Stop IDs on the simulated line (used for map highlighting). */
  private List<String> affectedStopIds;
}
