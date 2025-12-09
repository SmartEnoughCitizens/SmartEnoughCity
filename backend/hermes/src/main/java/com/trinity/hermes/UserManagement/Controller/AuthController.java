package com.trinity.hermes.UserManagement.Controller;

import com.trinity.hermes.UserManagement.dto.LoginRequest;
import com.trinity.hermes.UserManagement.dto.LoginResponse;
import com.trinity.hermes.UserManagement.Service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;


@RestController
@RequestMapping("/api/auth")
@Slf4j
@CrossOrigin(origins = "*") // Allow frontend to call this API
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
