package com.trinity.hermes.indicators.train.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "irish_rail_station_data", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainStationData {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "station_code")
    private String stationCode;

    @Column(name = "train_code")
    private String trainCode;

    @Column(name = "train_date")
    private LocalDate trainDate;

    @Column(name = "train_type")
    private String trainType;

    @Column(name = "origin")
    private String origin;

    @Column(name = "destination")
    private String destination;

    @Column(name = "origin_time")
    private LocalTime originTime;

    @Column(name = "destination_time")
    private LocalTime destinationTime;

    @Column(name = "status")
    private String status;

    @Column(name = "last_location")
    private String lastLocation;

    @Column(name = "due_in_minutes")
    private Integer dueInMinutes;

    @Column(name = "late_minutes")
    private Integer lateMinutes;

    @Column(name = "exp_arrival")
    private LocalTime expArrival;

    @Column(name = "exp_depart")
    private LocalTime expDepart;

    @Column(name = "sch_arrival")
    private LocalTime schArrival;

    @Column(name = "sch_depart")
    private LocalTime schDepart;

    @Column(name = "direction")
    private String direction;

    @Column(name = "location_type")
    private String locationType;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;
}
