package com.trinity.hermes.indicators.bus.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "bus_live_trip_updates", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusLiveTripUpdate {

  @Id
  @Column(name = "entry_id")
  private Integer entryId;

  @Column(name = "trip_id", nullable = false)
  private String tripId;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "schedule_relationship", columnDefinition = "varchar", nullable = false)
  private String scheduleRelationship;

  @Column(name = "direction_id", nullable = false)
  private Integer directionId;

  @Column(name = "vehicle_id", nullable = false)
  private Integer vehicleId;

  @Column(nullable = false)
  private Timestamp timestamp;
}
