package com.trinity.hermes.indicators.events.facade;

import com.trinity.hermes.indicators.events.dto.EventsDTO;
import com.trinity.hermes.indicators.events.service.EventsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventsFacade {

  private final EventsService eventsService;

  public List<EventsDTO> getUpcomingEvents(int limit) {
    return eventsService.getUpcomingEvents(limit);
  }
}
