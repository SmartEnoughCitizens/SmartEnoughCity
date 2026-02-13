package com.trinity.hermes.indicators.bus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "bus_live_trip_updates_stop_time_updates", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusLiveStopTimeUpdate {

  @Id
  @Column(name = "entry_id")
  private Integer entryId;

  @Column(name = "trip_update_entry_id", nullable = false)
  private Integer tripUpdateEntryId;

  @Column(name = "stop_id", nullable = false)
  private String stopId;

  @Column(name = "stop_sequence", nullable = false)
  private Integer stopSequence;

  @Column(name = "schedule_relationship", columnDefinition = "varchar", nullable = false)
  private String scheduleRelationship;

  @Column(name = "arrival_delay")
  private Integer arrivalDelay;

  @Column(name = "departure_delay")
  private Integer departureDelay;
}
