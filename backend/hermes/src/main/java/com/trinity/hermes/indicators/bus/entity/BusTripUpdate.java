package com.trinity.hermes.indicators.bus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "bus_trip_updates", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusTripUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "trip_id", nullable = false)
    private String tripId;

    @Column(name = "route_id", nullable = false)
    private String routeId;

    @Column(name = "start_time", nullable = false)
    private String startTime;

    @Column(name = "start_date", nullable = false)
    private String startDate;

    @Column(name = "stop_sequence", nullable = false)
    private String stopSequence;

    @Column(name = "stop_id", nullable = false)
    private String stopId;

    @Column(name = "arrival_delay")
    private Integer arrivalDelay;

    @Column(name = "departure_delay")
    private Integer departureDelay;

    @Column(name = "schedule_relationship")
    private String scheduleRelationship;
}