package com.trinity.hermes.indicators.tram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for POST /api/v1/tram/stop-demand/simulate. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramDemandSimulateRequestDTO {

  /** Tram line to add services to: "red" or "green". */
  private String line;

  /** Number of extra trams to add (1-20). */
  private int extraTrams;
}
