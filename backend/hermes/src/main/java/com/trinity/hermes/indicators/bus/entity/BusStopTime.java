package com.trinity.hermes.indicators.bus.entity;

import jakarta.persistence.*;
import java.sql.Time;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "bus_stop_times", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusStopTime {

  @Id
  @Column(name = "entry_id")
  private Integer entryId;

  @Column(name = "trip_id", nullable = false)
  private String tripId;

  @Column(name = "stop_id", nullable = false)
  private String stopId;

  @Column(name = "arrival_time", nullable = false)
  private Time arrivalTime;

  @Column(name = "departure_time", nullable = false)
  private Time departureTime;

  @Column(nullable = false)
  private Integer sequence;

  private String headsign;
}
