package com.trinity.hermes.indicators.train.repository;

/** Raw trip-count per Dublin stop — used to compute demand scores. */
public interface StationTripCountProjection {
  String getStopId();

  String getName();

  Double getLat();

  Double getLon();

  Integer getTripCount();

  /** Annual passenger boardings from train_station_ridership (2024). 0 if no match. */
  Integer getRidershipCount();

  /** Total population living within 800 m of this station (from CSO small areas). */
  Integer getCatchmentPopulation();

  /** Irish Rail station type: D=DART, S=Suburban, M=Mainline. Null if unmatched. */
  String getStationType();
}
