package com.trinity.hermes.indicators.bus.repository;

public interface BusNewStopRecommendationProjection {

  String getRouteId();

  String getRouteShortName();

  String getRouteLongName();

  String getStopAId();

  Integer getStopACode();

  String getStopAName();

  Double getStopALat();

  Double getStopALon();

  String getStopBId();

  Integer getStopBCode();

  String getStopBName();

  Double getStopBLat();

  Double getStopBLon();

  Double getCandidateLat();

  Double getCandidateLon();

  Double getPopulationScore();

  Double getPublicSpaceScore();

  Double getCombinedScore();
}
