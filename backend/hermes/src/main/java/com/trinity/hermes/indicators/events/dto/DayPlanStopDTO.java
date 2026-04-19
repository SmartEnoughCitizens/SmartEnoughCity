package com.trinity.hermes.indicators.events.dto;

import java.util.List;

public record DayPlanStopDTO(
    String stopId,
    String stopName,
    Double lat,
    Double lon,
    List<String> routes,
    Integer availableBikes,
    List<DayPlanEventRefDTO> events) {}
