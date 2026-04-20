package com.trinity.hermes.indicators.events.dto;

import com.trinity.hermes.disruptionmanagement.dto.AlternativeDTO;
import java.util.List;
import lombok.Builder;

/** Public-facing event detail DTO — returned for QR code landing pages. */
@Builder
public record EventPublicDTO(
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
    String riskLevel,
    List<AlternativeDTO> nearbyTransport) {}
