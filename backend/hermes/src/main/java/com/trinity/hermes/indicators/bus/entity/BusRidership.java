package com.trinity.hermes.indicators.bus.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "bus_ridership", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusRidership {

  @Id
  @Column(name = "entry_id")
  private Integer entryId;

  @Column(name = "vehicle_id", nullable = false)
  private Integer vehicleId;

  @Column(name = "trip_id", nullable = false)
  private String tripId;

  @Column(name = "nearest_stop_id", nullable = false)
  private String nearestStopId;

  @Column(name = "stop_sequence", nullable = false)
  private Integer stopSequence;

  @Column(nullable = false)
  private Timestamp timestamp;

  @Column(name = "passengers_boarding", nullable = false)
  private Integer passengersBoarding;

  @Column(name = "passengers_alighting", nullable = false)
  private Integer passengersAlighting;

  @Column(name = "passengers_onboard", nullable = false)
  private Integer passengersOnboard;

  @Column(name = "vehicle_capacity", nullable = false)
  private Integer vehicleCapacity;
}
