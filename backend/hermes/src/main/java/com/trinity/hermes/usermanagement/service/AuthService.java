package com.trinity.hermes.usermanagement.service;

import com.trinity.hermes.usermanagement.dto.LoginRequest;
import com.trinity.hermes.usermanagement.dto.LoginResponse;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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

  @Value("${keycloak.admin-username}")
  private String adminUsername;

  @Value("${keycloak.admin-password}")
  private String adminPassword;

  private final RestTemplate restTemplate;

  public AuthService() {
    this.restTemplate = new RestTemplate();
  }

  public LoginResponse login(LoginRequest loginRequest) {
    try {
      log.info("Attempting to authenticate user: {}", loginRequest.getUsername());

      // Prepare the token request to Keycloak
      String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

      log.info("Token URL: {}", tokenUrl);
      log.info("Client ID: {}", clientId);
      log.info("Realm: {}", realm);
      log.info(
          "Using client secret: {}",
          clientSecret != null
              ? "***" + clientSecret.substring(Math.max(clientSecret.length() - 4, 0))
              : "null");

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("client_id", clientId);
      body.add("client_secret", clientSecret);
      body.add("grant_type", "password");
      body.add("username", loginRequest.getUsername());
      body.add("password", loginRequest.getPassword());

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

      log.info("Sending request to Keycloak...");

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

  /** Get admin access token for Keycloak Admin API calls */
  private String getAdminToken() {
    try {
      String tokenUrl = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("client_id", "admin-cli");
      body.add("username", adminUsername);
      body.add("password", adminPassword);
      body.add("grant_type", "password");

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

      ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        String token = (String) response.getBody().get("access_token");
        log.info("Admin token obtained successfully");
        return token;
      }

      throw new RuntimeException("Failed to get admin token");

    } catch (Exception e) {
      log.error("Error getting admin token", e);
      throw new RuntimeException("Failed to get admin token: " + e.getMessage());
    }
  }

  /** Get all users from Keycloak realm */
  public List<Map<String, Object>> getAllUsers() {
    try {
      log.info("Fetching all users from realm: {}", realm);

      String adminToken = getAdminToken();
      String usersUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users";

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);

      HttpEntity<Void> request = new HttpEntity<>(headers);

      ResponseEntity<List<Map<String, Object>>> response =
          restTemplate.exchange(
              usersUrl,
              HttpMethod.GET,
              request,
              new ParameterizedTypeReference<List<Map<String, Object>>>() {});

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        log.info("Successfully fetched {} users", response.getBody().size());
        return response.getBody();
      }

      throw new RuntimeException("Failed to get users");

    } catch (Exception e) {
      log.error("Error getting all users", e);
      throw new RuntimeException("Failed to get users: " + e.getMessage());
    }
  }
}
