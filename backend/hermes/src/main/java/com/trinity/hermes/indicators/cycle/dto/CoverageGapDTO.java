package com.trinity.hermes.indicators.cycle.dto;

import java.time.Instant;
import lombok.Data;

@Data
public class CoverageGapDTO {

  /** Electoral division name — e.g. "Arran Quay A, Dublin City". */
  private String electoralDivision;

  /** Number of flats/apartments in this electoral division (key demand signal). */
  private int flatApartmentCount;

  /** Number of houses/bungalows in this electoral division. */
  private int houseBungalowCount;

  /** Total dwellings in this electoral division. */
  private int totalDwellings;

  /** Centroid latitude of the electoral division polygon. */
  private double centroidLat;

  /** Centroid longitude of the electoral division polygon. */
  private double centroidLon;

  /** Distance in metres to the nearest Dublin Bikes station. */
  private Double minDistanceM;

  /**
   * Coverage category:
   *
   * <ul>
   *   <li>NO_COVERAGE — flat count > 50 and nearest station > 1000 m
   *   <li>POOR_COVERAGE — flat count > 50 and nearest station 500–1000 m
   *   <li>ADEQUATE — all other areas
   * </ul>
   */
  private String coverageCategory;

  /** Composite priority score for sorting (higher = more urgent). */
  private int priorityScore;

  /** When this result was last computed. */
  private Instant computedAt;

  /** True once a city manager marks this area as planned for implementation. */
  private boolean processedForImplementation;

  /** When the area was marked as processed. Null if not yet processed. */
  private Instant processedAt;

  /** Simplified GeoJSON geometry string for polygon choropleth rendering on the map. */
  private String geomGeoJson;
}
