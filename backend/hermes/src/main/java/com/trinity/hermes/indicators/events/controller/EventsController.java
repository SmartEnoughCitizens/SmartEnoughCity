package com.trinity.hermes.indicators.events.controller;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.indicators.events.dto.EventsDTO;
import com.trinity.hermes.indicators.events.facade.EventsFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EventsController {

  private final EventsFacade eventsFacade;

  @GetMapping("/api/v1/events")
  public ResponseEntity<List<EventsDTO>> getUpcomingEvents(
      @RequestParam(defaultValue = "10") Integer limit) {
    log.info("GET /api/v1/events?limit={}", LogSanitizer.sanitizeLog(limit));
    try {
      return ResponseEntity.ok(eventsFacade.getUpcomingEvents(limit));
    } catch (Exception e) {
      log.error("Error fetching events: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
