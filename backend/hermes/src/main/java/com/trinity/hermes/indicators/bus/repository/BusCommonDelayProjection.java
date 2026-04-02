package com.trinity.hermes.indicators.bus.repository;

public interface BusCommonDelayProjection {
  String getRouteId();

  String getRouteShortName();

  String getRouteLongName();

  Double getAvgDelayMinutes();
}
