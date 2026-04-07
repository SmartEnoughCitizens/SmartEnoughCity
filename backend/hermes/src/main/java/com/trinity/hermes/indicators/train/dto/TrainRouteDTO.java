package com.trinity.hermes.indicators.train.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A single rail route corridor: human-readable name plus ordered lat/lon waypoints. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainRouteDTO {
  /** Full route name, e.g. "Bray - Howth" */
  private String routeName;

  /** Short category: "DART", "Commuter", "InterCity", "rail" */
  private String shortName;

  /**
   * Ordered list of [lat, lon] pairs forming the polyline. Serialises to {@code [[lat1, lon1],
   * [lat2, lon2], ...]} in JSON.
   */
  private List<double[]> stops;

  /** GTFS stop IDs parallel to {@code stops} — used by the frontend for demand colouring. */
  private List<String> stopIds;
}
