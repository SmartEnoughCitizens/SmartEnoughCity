package com.trinity.hermes.usermanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.hermes.usermanagement.dto.LoginRequest;
import com.trinity.hermes.usermanagement.dto.LoginResponse;
import java.util.Base64;
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

      Map<String, Object> tokenResponse = response.getBody();
      if (response.getStatusCode() == HttpStatus.OK && tokenResponse != null) {

        Object accessTokenObj = tokenResponse.get("access_token");
        Object tokenTypeObj = tokenResponse.get("token_type");
        Object expiresInObj = tokenResponse.get("expires_in");
        Object refreshTokenObj = tokenResponse.get("refresh_token");

        if (!(accessTokenObj instanceof String accessToken)
            || !(tokenTypeObj instanceof String tokenType)
            || !(expiresInObj instanceof Integer expiresIn)
            || !(refreshTokenObj instanceof String refreshToken)) {
          throw new RuntimeException("Invalid token response from Keycloak");
        }

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken(accessToken);
        loginResponse.setTokenType(tokenType);
        loginResponse.setExpiresIn(expiresIn);
        loginResponse.setRefreshToken(refreshToken);
        loginResponse.setUsername(loginRequest.getUsername());
        loginResponse.setMessage("Login successful");

        log.info("User {} authenticated successfully", loginRequest.getUsername());
        return loginResponse;
      } else {
        log.error("Failed to authenticate user: {}", loginRequest.getUsername());
        throw new RuntimeException("Authentication failed");
      }

    } catch (RuntimeException e) {
      log.error("Error during authentication for user: {}", loginRequest.getUsername(), e);
      throw e;
    }
  }

  public LoginResponse refreshToken(String refreshToken) {
    try {
      log.info("Attempting to refresh access token");

      String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("client_id", clientId);
      body.add("client_secret", clientSecret);
      body.add("grant_type", "refresh_token");
      body.add("refresh_token", refreshToken);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

      ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

      Map<String, Object> tokenResponse = response.getBody();
      if (response.getStatusCode() == HttpStatus.OK && tokenResponse != null) {

        Object accessTokenObj = tokenResponse.get("access_token");
        Object tokenTypeObj = tokenResponse.get("token_type");
        Object expiresInObj = tokenResponse.get("expires_in");
        Object refreshTokenObj = tokenResponse.get("refresh_token");

        if (!(accessTokenObj instanceof String accessToken)
            || !(tokenTypeObj instanceof String tokenType)
            || !(expiresInObj instanceof Integer expiresIn)
            || !(refreshTokenObj instanceof String newRefreshToken)) {
          throw new RuntimeException("Invalid token response from Keycloak");
        }

        String username = extractUsernameFromJwt(accessToken);

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken(accessToken);
        loginResponse.setTokenType(tokenType);
        loginResponse.setExpiresIn(expiresIn);
        loginResponse.setRefreshToken(newRefreshToken);
        loginResponse.setUsername(username);
        loginResponse.setMessage("Token refreshed successfully");

        log.info("Access token refreshed successfully");
        return loginResponse;
      } else {
        throw new RuntimeException("Token refresh failed");
      }

    } catch (RuntimeException e) {
      log.error("Error during token refresh", e);
      throw e;
    }
  }

  private String extractUsernameFromJwt(String jwtToken) {
    try {
      int firstDot = jwtToken.indexOf('.');
      int secondDot = jwtToken.indexOf('.', firstDot + 1);
      String payloadBase64 = jwtToken.substring(firstDot + 1, secondDot);
      byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadBase64);
      @SuppressWarnings("unchecked")
      Map<String, Object> claims = new ObjectMapper().readValue(payloadBytes, Map.class);
      Object preferred = claims.get("preferred_username");
      return preferred instanceof String s ? s : null;
    } catch (Exception e) {
      log.warn("Could not extract username from JWT", e);
      return null;
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

      Map<String, Object> responseBody = response.getBody();
      if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {

        Object tokenObj = responseBody.get("access_token");
        if (!(tokenObj instanceof String token)) {
          throw new RuntimeException("Admin access_token missing in Keycloak response");
        }

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
      if (adminToken == null) {
        throw new IllegalStateException("Admin token is null");
      }
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

      List<Map<String, Object>> users = response.getBody();

      if (response.getStatusCode() == HttpStatus.OK && users != null) {
        log.info("Successfully fetched {} users", users.size());
        return users;
      }

      throw new RuntimeException("Failed to get users");

    } catch (Exception e) {
      log.error("Error getting all users", e);
      throw new RuntimeException("Failed to get users: " + e.getMessage());
    }
  }
}
