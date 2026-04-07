package com.trinity.hermes.indicators.train.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Demand score for a single Dublin train station. Deserialized from snake_case (inference engine)
 * via @JsonAlias; serialized to camelCase (frontend) using default Jackson behaviour.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrainStationDemandDTO {
  @JsonAlias("stop_id")
  private String stopId;

  private String name;
  private double lat;
  private double lon;

  @JsonAlias("trip_count")
  private int tripCount;

  @JsonAlias("ridership_count")
  private int ridershipCount;

  @JsonAlias("catchment_population")
  private int catchmentPopulation;

  @JsonAlias("station_type")
  private String stationType;

  @JsonAlias("footfall_count")
  private int footfallCount;

  @JsonAlias("norm_ridership")
  private double normRidership;

  @JsonAlias("norm_uptake")
  private double normUptake;

  @JsonAlias("norm_pressure")
  private double normPressure;

  @JsonAlias("norm_footfall")
  private double normFootfall;

  @JsonAlias("raw_pressure")
  private double rawPressure;

  @JsonAlias("max_pressure")
  private double maxPressure;

  @JsonAlias("demand_score")
  private double demandScore;
}
