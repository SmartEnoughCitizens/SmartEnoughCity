package com.trinity.hermes.indicators.events.service;

import com.trinity.hermes.indicators.events.dto.EventsDTO;
import com.trinity.hermes.indicators.events.entity.Events;
import com.trinity.hermes.indicators.events.repository.EventsRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventsService {

  private final EventsRepository eventsRepository;

  @Transactional(readOnly = true)
  public List<EventsDTO> getUpcomingEvents(int limit) {
    log.debug("Fetching up to {} upcoming events", limit);
    return eventsRepository
        .findUpcomingEvents(PageRequest.of(0, limit))
        .stream()
        .map(this::mapToDTO)
        .collect(Collectors.toList());
  }

  private EventsDTO mapToDTO(Events e) {
    return EventsDTO.builder()
        .id(e.getId())
        .eventName(e.getEventName())
        .eventType(e.getEventType())
        .venueName(e.getVenueName())
        .latitude(e.getLatitude())
        .longitude(e.getLongitude())
        .eventDate(e.getEventDate())
        .startTime(e.getStartTime())
        .endTime(e.getEndTime())
        .estimatedAttendance(e.getEstimatedAttendance())
        .build();
  }
}
