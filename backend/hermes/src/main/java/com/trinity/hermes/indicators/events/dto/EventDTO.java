package com.trinity.hermes.indicators.events.dto;

import lombok.Builder;

/** Response DTO for a single upcoming city event. */
@Builder
public record EventDTO(
    Integer id,
    String eventName,
    String eventType,
    String venueName,
    Integer venueCapacity,
    Double latitude,
    Double longitude,
    String eventDate,
    String startTime,
    String endTime,
    Integer estimatedAttendance,
    /** Derived from venue capacity: CRITICAL / HIGH / MEDIUM / LOW. */
    String riskLevel) {}
