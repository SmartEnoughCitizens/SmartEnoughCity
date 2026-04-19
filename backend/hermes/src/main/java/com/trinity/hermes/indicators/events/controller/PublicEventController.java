package com.trinity.hermes.indicators.events.controller;

import com.trinity.hermes.disruptionmanagement.dto.AlternativeDTO;
import com.trinity.hermes.disruptionmanagement.dto.AlternativeTransportResult;
import com.trinity.hermes.disruptionmanagement.service.AlternativeTransportService;
import com.trinity.hermes.indicators.events.dto.EventPublicDTO;
import com.trinity.hermes.indicators.events.entity.Events;
import com.trinity.hermes.indicators.events.repository.EventsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (no-auth) event endpoint — accessible without a JWT for QR code landing pages.
 *
 * <p>All paths under {@code /api/public/**} are permit-all in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/public/events")
@RequiredArgsConstructor
public class PublicEventController {

  private final EventsRepository eventsRepository;
  private final AlternativeTransportService alternativeTransportService;

  /**
   * Returns full event detail plus nearby transport alternatives. Used as the QR code landing page
   * endpoint for event attendees.
   *
   * <p>GET /api/public/events/{id}
   */
  @GetMapping("/{id}")
  public ResponseEntity<EventPublicDTO> getPublicEvent(@PathVariable Integer id) {
    return eventsRepository
        .findByIdWithVenue(id)
        .map(
            event -> {
              List<AlternativeDTO> nearby = buildNearbyTransport(event);
              return ResponseEntity.ok(toDto(event, nearby));
            })
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private List<AlternativeDTO> buildNearbyTransport(Events event) {
    if (event.getLatitude() == null || event.getLongitude() == null) return List.of();
    List<AlternativeTransportResult> results =
        alternativeTransportService.findNearby(event.getLatitude(), event.getLongitude());
    return results.stream()
        .map(
            r ->
                AlternativeDTO.builder()
                    .mode(r.transportType())
                    .stopName(r.stopName())
                    .description(buildDescription(r))
                    .availabilityCount(r.availableBikes())
                    .lat(r.lat())
                    .lon(r.lon())
                    .googleMapsWalkingUrl(
                        buildWalkingUrl(
                            event.getLatitude(), event.getLongitude(), r.lat(), r.lon()))
                    .build())
        .toList();
  }

  private String buildDescription(AlternativeTransportResult r) {
    return switch (r.transportType()) {
      case "bus" -> "Bus stop: " + r.stopName() + " (" + r.distanceM() + "m away)";
      case "rail" -> "Irish Rail: " + r.stopName() + " (" + r.distanceM() + "m away)";
      case "tram" -> "Luas: " + r.stopName() + " (" + r.distanceM() + "m away)";
      case "bike" ->
          "DublinBikes: "
              + r.stopName()
              + " — "
              + (r.availableBikes() != null ? r.availableBikes() : 0)
              + " bikes ("
              + r.distanceM()
              + "m away)";
      default -> r.stopName() + " (" + r.distanceM() + "m away)";
    };
  }

  private String buildWalkingUrl(double fromLat, double fromLon, Double toLat, Double toLon) {
    if (toLat == null || toLon == null) return null;
    return String.format(
        "https://www.google.com/maps/dir/?api=1&origin=%s,%s&destination=%s,%s&travelmode=walking",
        fromLat, fromLon, toLat, toLon);
  }

  private EventPublicDTO toDto(Events e, List<AlternativeDTO> nearbyTransport) {
    Integer capacity = e.getVenue() != null ? e.getVenue().getCapacity() : null;
    return EventPublicDTO.builder()
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
        .nearbyTransport(nearbyTransport)
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
