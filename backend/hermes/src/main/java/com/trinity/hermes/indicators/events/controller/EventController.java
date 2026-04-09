package com.trinity.hermes.indicators.events.controller;

import com.trinity.hermes.indicators.events.dto.EventDTO;
import com.trinity.hermes.indicators.events.service.EventsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for upcoming city events. Used by the disruption dashboard's Events tab to let city
 * managers view high-capacity events and plan transport resources in advance.
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

  private final EventsService eventsService;

  /**
   * Returns all upcoming events within the next {@code days} days (default 7). Response includes
   * venue capacity and a pre-computed riskLevel (CRITICAL/HIGH/MEDIUM/LOW) so the frontend can
   * colour-code markers without its own capacity logic.
   *
   * <p>GET /api/v1/events?days=7
   */
  @GetMapping
  public ResponseEntity<List<EventDTO>> getUpcomingEvents(
      @RequestParam(defaultValue = "7") int days) {
    return ResponseEntity.ok(eventsService.getUpcomingEvents(days));
  }
}
