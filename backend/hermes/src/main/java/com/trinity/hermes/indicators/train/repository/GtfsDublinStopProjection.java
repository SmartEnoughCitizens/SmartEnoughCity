package com.trinity.hermes.indicators.train.repository;

/** GTFS stop within the Dublin bounding box, optionally enriched with station_type. */
public interface GtfsDublinStopProjection {
  String getId();

  String getName();

  Double getLat();

  Double getLon();

  /** Joined from irish_rail_stations on name match — may be null. */
  String getStationType();
}
