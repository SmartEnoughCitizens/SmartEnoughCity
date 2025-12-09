package com.trinity.hermes.dataanalyzer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "cycle_stations", schema = "external_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CycleStation {

    @Id
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
}