package com.trinity.hermes.indicators.events.service;

import com.trinity.hermes.indicators.events.dto.EventDTO;
import com.trinity.hermes.indicators.events.entity.Events;
import com.trinity.hermes.indicators.events.repository.EventsRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventsService {

  private final EventsRepository eventsRepository;

  /**
   * Returns all upcoming events within the next {@code days} days, with venue capacity included for
   * risk-level colouring on the city dashboard.
   */
  public List<EventDTO> getUpcomingEvents(int days) {
    List<Events> events =
        eventsRepository.findUpcomingEventsDays(
            LocalDate.now(ZoneId.of("Europe/Dublin")).plusDays(days), PageRequest.of(0, 500));
    return events.stream().map(this::toDto).toList();
  }

  private EventDTO toDto(Events e) {
    Integer capacity = e.getVenue() != null ? e.getVenue().getCapacity() : null;
    return EventDTO.builder()
        .id(e.getId())
        .eventName(e.getEventName())
        .eventType(e.getEventType())
        .venueName(e.getVenueName())
        .venueCapacity(capacity)
        .latitude(e.getLatitude())
        .longitude(e.getLongitude())
        .eventDate(e.getEventDate() != null ? e.getEventDate().toString() : null)
        .startTime(e.getStartTime() != null ? e.getStartTime().toString() : null)
        .endTime(e.getEndTime() != null ? e.getEndTime().toString() : null)
        .estimatedAttendance(e.getEstimatedAttendance())
        .riskLevel(scoreRisk(capacity))
        .build();
  }

  private String scoreRisk(Integer capacity) {
    if (capacity == null) return "LOW";
    if (capacity >= 15_000) return "CRITICAL";
    if (capacity >= 5_000) return "HIGH";
    if (capacity >= 1_000) return "MEDIUM";
    return "LOW";
  }
}
