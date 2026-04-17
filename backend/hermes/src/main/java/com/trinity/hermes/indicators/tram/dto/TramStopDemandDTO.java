package com.trinity.hermes.indicators.tram.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Per-stop demand score used for simulation visualisation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TramStopDemandDTO {

  private String stopId;
  private String stopName;
  private String line;
  private Double lat;
  private Double lon;

  /** Total daily trips passing through this stop (from GTFS). */
  private int tripCount;

  /** Normalised demand score 0.0 (low) – 1.0 (high). */
  private double demandScore;
}
