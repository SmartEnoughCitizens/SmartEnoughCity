package com.trinity.hermes.disruptionmanagement.controller;

import com.trinity.hermes.disruptionmanagement.dto.AlternativeDTO;
import com.trinity.hermes.disruptionmanagement.dto.AlternativeTransportResult;
import com.trinity.hermes.disruptionmanagement.dto.DisruptionResponse;
import com.trinity.hermes.disruptionmanagement.facade.DisruptionFacade;
import com.trinity.hermes.disruptionmanagement.service.AlternativeTransportService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (no-auth) disruption endpoints — accessible without a JWT, suitable for QR code landing
 * pages and on-screen displays in disrupted areas.
 *
 * <p>All paths under {@code /api/public/**} are permit-all in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/public/disruptions")
@RequiredArgsConstructor
@Slf4j
public class PublicDisruptionController {

  @SuppressFBWarnings(
      value = "EI2",
      justification = "Spring-injected facade dependency stored in controller field")
  private final DisruptionFacade disruptionFacade;

  private final AlternativeTransportService alternativeTransportService;

  /**
   * Returns the full disruption detail for the given ID without requiring authentication. Used as
   * the QR code landing page endpoint.
   *
   * <p>GET /api/public/disruptions/{id}
   */
  @GetMapping("/{id}")
  public ResponseEntity<DisruptionResponse> getDisruption(@PathVariable Long id) {
    return disruptionFacade
        .getDisruptionById(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Returns nearby alternative transport for any lat/lon point without authentication. Used by the
   * events tab to show transport options near a venue.
   *
   * <p>GET /api/public/disruptions/alternatives?lat={lat}&lon={lon}
   */
  @GetMapping("/alternatives")
  public ResponseEntity<List<AlternativeDTO>> getNearbyAlternatives(
      @RequestParam double lat, @RequestParam double lon) {
    List<AlternativeTransportResult> results = alternativeTransportService.findNearby(lat, lon);
    List<AlternativeDTO> dtos =
        results.stream()
            .map(
                r ->
                    AlternativeDTO.builder()
                        .mode(r.transportType())
                        .stopName(r.stopName())
                        .description(buildDescription(r))
                        .availabilityCount(r.availableBikes())
                        .lat(r.lat())
                        .lon(r.lon())
                        .googleMapsWalkingUrl(buildWalkingUrl(lat, lon, r.lat(), r.lon()))
                        .build())
            .toList();
    return ResponseEntity.ok(dtos);
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
}
