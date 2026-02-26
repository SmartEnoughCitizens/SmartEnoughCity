package com.trinity.hermes.usermanagement.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.hermes.usermanagement.common.TestUtils;
import com.trinity.hermes.usermanagement.dto.RegisterUserRequest;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserManagementTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("smart_enough_city")
          .withUsername("backend_user")
          .withPassword("dev_backend_user_password");

  @Container
  static KeycloakContainer keycloak =
      new KeycloakContainer("quay.io/keycloak/keycloak:26.0.0")
          .withRealmImportFile("realm-test.json");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
    r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    r.add("spring.jpa.properties.hibernate.default_schema", () -> "public");
    r.add("spring.liquibase.enabled", () -> "false");

    r.add(
        "spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> keycloak.getAuthServerUrl().replaceAll("/$", "") + "/realms/user-management-realm");

    r.add("keycloak.server-url", keycloak::getAuthServerUrl);
    r.add("keycloak.realm", () -> "user-management-realm");
    r.add("keycloak.client-id", () -> "user-service");
    r.add("keycloak.client-secret", () -> "dev-keycloak-user-service-secret");

    r.add("keycloak.admin-username", keycloak::getAdminUsername);
    r.add("keycloak.admin-password", keycloak::getAdminPassword);
  }

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  private String token(String username, String password) {
    String tokenUrl =
        keycloak.getAuthServerUrl().replaceAll("/$", "")
            + "/realms/user-management-realm/protocol/openid-connect/token";

    var client = new org.springframework.web.client.RestTemplate();

    var headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

    var body = new org.springframework.util.LinkedMultiValueMap<String, String>();
    body.add("grant_type", "password");
    body.add("client_id", "frontend-app");
    body.add("username", username);
    body.add("password", password);

    var req = new org.springframework.http.HttpEntity<>(body, headers);

    @SuppressWarnings("unchecked")
    Map<String, Object> resp;
    try {
      resp = client.postForObject(tokenUrl, req, Map.class);
    } catch (org.springframework.web.client.RestClientResponseException e) {
      throw new IllegalStateException(
          "Failed to fetch token for user '"
              + username
              + "'. HTTP "
              + e.getRawStatusCode()
              + " body="
              + e.getResponseBodyAsString(),
          e);
    }

    if (resp == null) {
      throw new IllegalStateException(
          "Token endpoint returned null response for user '" + username + "'");
    }

    Object token = resp.get("access_token");
    if (!(token instanceof String accessToken) || accessToken.isBlank()) {
      throw new IllegalStateException(
          "Token endpoint did not return access_token for user '"
              + username
              + "'. Response="
              + resp);
    }
    return accessToken;
  }

  private RegisterUserRequest registerReq(String username, String role) {
    RegisterUserRequest r = new RegisterUserRequest();
    r.setUsername(username);
    r.setEmail(username + "@mail.com");
    r.setFirstName("First");
    r.setLastName("Last");
    r.setRole(role);
    r.setPassword(TestUtils.randomPassword());
    return r;
  }

  @Test
  void cycleAdmin_canCreate_cycleProvider_201() throws Exception {
    String accessToken = token("cycle_admin", "CycleAdmin@123");

    mockMvc
        .perform(
            post("/api/usermanagement/register")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq("cp_it_1", "Cycle_Provider"))))
        .andExpect(status().isCreated());
  }

  @Test
  void cityManager_cannotCreate_cycleProvider_403() throws Exception {
    String accessToken = token("city_manager", "CityManager@123");
    debugToken(accessToken);

    mockMvc
        .perform(
            post("/api/usermanagement/register")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq("cp_it_2", "Cycle_Provider"))))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  void govAdmin_canListUsers_200() throws Exception {
    String accessToken = token("gov_admin", "GovAdmin@123");
    debugToken(accessToken);

    mockMvc
        .perform(get("/api/usermanagement/users").header("Authorization", "Bearer " + accessToken))
        .andDo(print())
        .andExpect(status().isOk());
  }

  private void debugToken(String accessToken) {
    // Use issuer-uri JWKS
    String jwkSetUri =
        keycloak.getAuthServerUrl().replaceAll("/$", "")
            + "/realms/user-management-realm/protocol/openid-connect/certs";

    JwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    Jwt jwt = decoder.decode(accessToken);

    System.out.println("preferred_username = " + jwt.getClaimAsString("preferred_username"));
    System.out.println("realm_access = " + jwt.getClaim("realm_access"));
  }
}
