package com.trinity.hermes.usermanagement.controller;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class UserManagementController {

  // Public endpoint - no authentication needed
  @GetMapping("/public/health")
  public ResponseEntity<Map<String, String>> health() {
    log.info("Accessing Public API");
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("message", "Hermes Service is running!");
    return ResponseEntity.ok(response);
  }

  // Trains - Only City_Manager can access
  @GetMapping("/trains")
  @PreAuthorize("hasRole('City_Manager')")
  public ResponseEntity<Map<String, Object>> getTrains() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    log.info("User {} (roles: {}) accessing Trains API", auth.getName(), auth.getAuthorities());

    Map<String, Object> response = new HashMap<>();
    response.put("resource", "Trains");
    response.put("message", "Successfully accessed Trains API");
    response.put("description", "This resource is accessible ONLY by City_Manager role");

    return ResponseEntity.ok(response);
  }

  // Buses - Both City_Manager and Bus_Provider can access
  @GetMapping("/buses")
  @PreAuthorize("hasAnyRole('City_Manager', 'Bus_Provider')")
  public ResponseEntity<Map<String, Object>> getBuses() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    log.info("User {} (roles: {}) accessing Buses API", auth.getName(), auth.getAuthorities());

    Map<String, Object> response = new HashMap<>();
    response.put("resource", "Buses");
    response.put("message", "Successfully accessed Buses API");
    response.put(
        "description", "This resource is accessible by both City_Manager and Bus_Provider roles");

    return ResponseEntity.ok(response);
  }
}
