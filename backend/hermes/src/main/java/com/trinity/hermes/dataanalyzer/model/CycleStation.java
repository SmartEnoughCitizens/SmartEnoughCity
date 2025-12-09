package com.trinity.hermes.dataanalyzer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "cycle_stations", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CycleStation {

    @Id
    @Column(name = "station_id", nullable = false)
    private String stationId;

    private String name;
    private String address;
    private Integer capacity;
    private Integer numBikesAvailable;
    private Integer numDocksAvailable;
    private Boolean isInstalled;
    private Boolean isRenting;
    private Boolean isReturning;
    private Long lastReported;
    private String lastReportedDt;
    private Double lat;
    private Double lon;

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
}