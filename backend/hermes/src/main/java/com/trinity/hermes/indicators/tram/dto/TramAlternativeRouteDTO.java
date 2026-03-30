package com.trinity.hermes.indicators.tram.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TramAlternativeRouteDTO {
    private String transportType;
    private String stopId;
    private String stopName;
    private Double lat;
    private Double lon;
    private Integer distanceM;
    private Integer availableBikes;
    private Integer capacity;
}