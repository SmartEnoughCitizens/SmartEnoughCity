package com.trinity.hermes.dataanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CycleStationDTO {

    private Long id;
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

    // Calculated field
    private Double occupancyRate;

    public Double calculateOccupancyRate() {
        if (capacity != null && capacity > 0 && numBikesAvailable != null) {
            return (numBikesAvailable.doubleValue() / capacity.doubleValue()) * 100;
        }
        return 0.0;
    }
}