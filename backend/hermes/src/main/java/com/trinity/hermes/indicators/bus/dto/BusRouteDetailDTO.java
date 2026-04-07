package com.trinity.hermes.indicators.bus.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Route metadata, polyline from the first trip's shape, and scheduled stops for that same trip
 * (lexicographically smallest trip id for the route).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusRouteDetailDTO {

  private String routeId;
  private Integer agencyId;
  private String shortName;
  private String longName;

  /**
   * Trip used for {@link #shapeId}, {@link #shape}, and {@link #stops} (first by trip id for this
   * route).
   */
  private String representativeTripId;

  private String shapeId;
  private List<BusRouteShapePointDTO> shape;

  /** Stop pattern for {@link #representativeTripId}, ordered by sequence. */
  private List<BusRouteStopDTO> stops;
}
