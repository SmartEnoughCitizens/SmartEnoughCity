package com.trinity.hermes.dataanalyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cycle_stations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CycleStation {

    @Column(name = "station_id", nullable = false)
    private String stationId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "num_bikes_available")
    private Integer numBikesAvailable;

    @Column(name = "num_docks_available")
    private Integer numDocksAvailable;

    @Column(name = "is_installed")
    private Boolean isInstalled;

    @Column(name = "is_renting")
    private Boolean isRenting;

    @Column(name = "is_returning")
    private Boolean isReturning;

    @Column(name = "last_reported")
    private Long lastReported;

    @Column(name = "last_reported_dt")
    private String lastReportedDt;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}