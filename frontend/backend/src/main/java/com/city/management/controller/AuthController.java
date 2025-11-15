package com.city.management.controller;

import com.city.management.model.JwtResponse;
import com.city.management.model.LoginRequest;
import com.city.management.model.RegisterRequest;
import com.city.management.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<JwtResponse> response = authService.login(loginRequest.getUsername(), loginRequest.getPassword());

        if (response.isEmpty()) {
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid username or password"));
        }

        return ResponseEntity.ok(response.get());
    }

    @PostMapping("/register/city-manager")
    public ResponseEntity<?> registerCityManager(@RequestBody RegisterRequest registerRequest) {
        Optional<JwtResponse> response = authService.registerCityManager(registerRequest);

        if (response.isEmpty()) {
            return ResponseEntity.status(400).body(new ErrorResponse("Username already exists"));
        }

        return ResponseEntity.ok(response.get());
    }

    @PostMapping("/register/service-provider-admin")
    public ResponseEntity<?> registerServiceProviderAdmin(@RequestBody RegisterRequest registerRequest) {
        Optional<JwtResponse> response = authService.registerServiceProviderAdmin(registerRequest);

        if (response.isEmpty()) {
            return ResponseEntity.status(400).body(new ErrorResponse("Username already exists"));
        }

        return ResponseEntity.ok(response.get());
    }

    // Simple error response class
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
