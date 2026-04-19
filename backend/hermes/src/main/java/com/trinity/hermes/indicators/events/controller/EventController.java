package com.trinity.hermes.indicators.events.controller;

import com.trinity.hermes.indicators.events.dto.DayPlanDTO;
import com.trinity.hermes.indicators.events.dto.EventDTO;
import com.trinity.hermes.indicators.events.service.DayPlanService;
import com.trinity.hermes.indicators.events.service.EventsService;
import java.time.LocalDate;
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
  private final DayPlanService dayPlanService;

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

  /**
   * Returns a transport-centric day plan for the given date. For each mode (bus, tram, rail, bike),
   * lists which stops are within 500 m of an event, the routes serving each stop, and which events
   * are nearby. Used by the Events tab "Export Day Plan" feature.
   *
   * <p>GET /api/v1/events/day-plan?date=2025-06-01
   */
  @GetMapping("/day-plan")
  public ResponseEntity<DayPlanDTO> getDayPlan(@RequestParam String date) {
    LocalDate localDate = LocalDate.parse(date);
    return ResponseEntity.ok(dayPlanService.getDayPlan(localDate));
  }
}
