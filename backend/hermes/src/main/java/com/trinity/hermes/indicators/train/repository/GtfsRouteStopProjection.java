package com.trinity.hermes.indicators.train.repository;

/** One stop in a GTFS route polyline. */
public interface GtfsRouteStopProjection {
  String getRouteId();

  String getRouteName();

  String getShortName();

  String getStopId();

  Integer getLocationOrder();

  Double getLat();

  Double getLon();
}
