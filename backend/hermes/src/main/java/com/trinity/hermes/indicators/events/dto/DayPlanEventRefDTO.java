package com.trinity.hermes.indicators.events.dto;

public record DayPlanEventRefDTO(
    Integer id,
    String eventName,
    String venueName,
    String startTime,
    String riskLevel,
    int distanceM) {}
