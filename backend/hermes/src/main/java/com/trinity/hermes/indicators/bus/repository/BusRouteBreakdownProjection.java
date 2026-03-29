package com.trinity.hermes.indicators.bus.repository;

public interface BusRouteBreakdownProjection {
  String getStopId();

  Double getAvgDelayMinutes();

  Double getMaxDelayMinutes();

  Long getTripCount();
}
