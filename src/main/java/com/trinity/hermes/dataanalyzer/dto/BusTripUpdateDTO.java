package com.trinity.hermes.dataanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusTripUpdateDTO {

    private Long id;
    private String entityId;
    private String tripId;
    private String routeId;
    private String startTime;
    private String startDate;
    private String stopSequence;
    private String stopId;
    private Integer arrivalDelay;
    private Integer departureDelay;
    private String scheduleRelationship;
}