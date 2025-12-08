package com.trinity.hermes.UserManagement.Service;

import com.trinity.hermes.UserManagement.dto.LoginRequest;
import com.trinity.hermes.UserManagement.dto.LoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class AuthService {

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    public AuthService() {
        this.restTemplate = new RestTemplate();
    }

    public LoginResponse login(LoginRequest loginRequest) {
        try {
            log.info("Attempting to authenticate user: {}", loginRequest.getUsername());

            // Prepare the token request to Keycloak
            String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("grant_type", "password");
            body.add("username", loginRequest.getUsername());
            body.add("password", loginRequest.getPassword());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            // Call Keycloak token endpoint
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();

                LoginResponse loginResponse = new LoginResponse();
                loginResponse.setAccessToken((String) tokenResponse.get("access_token"));
                loginResponse.setTokenType((String) tokenResponse.get("token_type"));
                loginResponse.setExpiresIn((Integer) tokenResponse.get("expires_in"));
                loginResponse.setRefreshToken((String) tokenResponse.get("refresh_token"));
                loginResponse.setUsername(loginRequest.getUsername());
                loginResponse.setMessage("Login successful");

                log.info("User {} authenticated successfully", loginRequest.getUsername());
                return loginResponse;
            } else {
                log.error("Failed to authenticate user: {}", loginRequest.getUsername());
                throw new RuntimeException("Authentication failed");
            }

        } catch (Exception e) {
            log.error("Error during authentication for user: {}", loginRequest.getUsername(), e);
            throw new RuntimeException("Invalid username or password");
        }
    }
}
