package com.trinity.hermes.disruptionmanagement.controller;

import com.trinity.hermes.disruptionmanagement.dto.DisruptionResponse;
import com.trinity.hermes.disruptionmanagement.facade.DisruptionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
