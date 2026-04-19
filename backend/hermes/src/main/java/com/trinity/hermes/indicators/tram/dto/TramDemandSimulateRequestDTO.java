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

  /** Number of trams to add (+) or remove (-). Range: -20 to +20. */
  private int extraTrams;

  /** Origin stop ID — only stops between origin and destination are affected. */
  private String originStopId;

  /** Destination stop ID — only stops between origin and destination are affected. */
  private String destinationStopId;

  /** Start hour of time period (e.g. 7 for Morning Peak). */
  private int startHour;

  /** End hour of time period (e.g. 10 for Morning Peak). */
  private int endHour;
}
