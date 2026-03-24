package com.trinity.hermes.usermanagement.controller;

import com.trinity.hermes.usermanagement.dto.ForgotPasswordRequest;
import com.trinity.hermes.usermanagement.dto.LoginRequest;
import com.trinity.hermes.usermanagement.dto.LoginResponse;
import com.trinity.hermes.usermanagement.dto.RefreshTokenRequest;
import com.trinity.hermes.usermanagement.dto.ResetPasswordRequest;
import com.trinity.hermes.usermanagement.service.AuthService;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@CrossOrigin(origins = "*") // Allow frontend to call this API
public class AuthController {

  private final AuthService authService;
  private final UserManagementService userManagementService;

  public AuthController(AuthService authService, UserManagementService userManagementService) {
    this.authService = authService;
    this.userManagementService = userManagementService;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
    try {
      log.info("Login request received for user: {}", loginRequest.getUsername());

      if (loginRequest.getUsername() == null || loginRequest.getUsername().isEmpty()) {
        return ResponseEntity.badRequest().body(createErrorResponse("Username is required"));
      }

      if (loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
        return ResponseEntity.badRequest().body(createErrorResponse("Password is required"));
      }

      // Authenticate with Keycloak
      LoginResponse response = authService.login(loginRequest);
      return ResponseEntity.ok(response);

    } catch (RuntimeException e) {
      log.error("Login failed for user: {}", loginRequest.getUsername(), e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(createErrorResponse("Invalid username or password"));
    } catch (Exception e) {
      log.error("Unexpected error during login", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse("An error occurred during login"));
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
    try {
      if (request.getRefreshToken() == null || request.getRefreshToken().isEmpty()) {
        return ResponseEntity.badRequest().body(createErrorResponse("Refresh token is required"));
      }
      LoginResponse response = authService.refreshToken(request.getRefreshToken());
      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      log.error("Token refresh failed", e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(createErrorResponse("Session expired. Please log in again."));
    }
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<Map<String, String>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {
    try {
      userManagementService.initiateForgotPassword(request.getEmail());
    } catch (Exception e) {
      log.error("Error during forgot password flow", e);
      // Intentionally swallowed — always return the same message to prevent email enumeration
    }
    Map<String, String> response = new HashMap<>();
    response.put("message", "If that email is registered, you will receive a reset link shortly.");
    return ResponseEntity.ok(response);
  }

  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    try {
      userManagementService.resetPassword(request.getToken(), request.getNewPassword());
      Map<String, String> response = new HashMap<>();
      response.put("message", "Password reset successfully. You may now log in.");
      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      log.warn("Password reset attempt failed: {}", e.getMessage());
      return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
    }
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("message", "Auth service is running");
    return ResponseEntity.ok(response);
  }

  private Map<String, String> createErrorResponse(String message) {
    Map<String, String> error = new HashMap<>();
    error.put("error", message);
    return error;
  }
}
