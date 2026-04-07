package com.trinity.hermes.indicators.train.repository;

/** Flat projection row: one stop on one route, with coordinates. */
public interface TrainRouteStopProjection {
  String getTrainOrigin();

  String getTrainDestination();

  /** Stop sequence within the route — used for ordering the polyline correctly. */
  Integer getLocationOrder();

  Double getLat();

  Double getLon();
}
