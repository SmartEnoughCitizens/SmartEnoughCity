package com.trinity.hermes.indicators.train.repository;

/**
 * Spring Data JPA projection for the frequent-delay native query. Column aliases in the SQL must
 * match these getter names (snake_case → camelCase).
 */
public interface TrainDelayProjection {
  String getTrainCode();

  String getOrigin();

  String getDestination();

  String getDirection();

  Double getTotalAvgDelayMinutes();
}
