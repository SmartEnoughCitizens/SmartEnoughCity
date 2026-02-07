package com.trinity.hermes.usermanagement.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.hermes.usermanagement.dto.RegisterUserRequest;
import com.trinity.hermes.usermanagement.dto.RegisterUserResponse;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserManagementController.class)
@Import(UserManagementControllerTest.TestSecurityConfig.class)
public class UserManagementControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean UserManagementService userManagementService;

  // -------------------------
  // Minimal security for tests (so jwt() works)
  // -------------------------
  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      return http.csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
          .build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
      // Dummy decoder: it will never be called when using
      // SecurityMockMvcRequestPostProcessors.jwt()
      return token -> {
        throw new JwtException("Not used in tests");
      };
    }
  }

  @BeforeEach
  void clearInvocations() {
    Mockito.clearInvocations(userManagementService);
  }

  // -------------------------
  // Helper methods
  // -------------------------
  private Jwt buildJwt(String preferredUsername, String... realmRoles) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("preferred_username", preferredUsername)
        // typical Keycloak structure:
        .claim("realm_access", Map.of("roles", List.of(realmRoles)))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  private RegisterUserRequest buildRegisterRequest(String username, String role) {
    RegisterUserRequest req = new RegisterUserRequest();
    // adjust setters/fields to match your DTO
    req.setUsername(username);
    req.setFirstName("Test"); // ✅ add
    req.setLastName("User");
    req.setRole(role);
    req.setEmail(username + "@mail.com");
    req.setPassword("Test@12345");
    return req;
  }

  private void mockHasRoleBasedOnJwtClaims() {
    // Your controller calls: userManagementService.hasRole(jwt, "SomeRole")
    // We simulate it by reading realm_access.roles from the Jwt passed in.
    when(userManagementService.hasRole(any(Jwt.class), anyString()))
        .thenAnswer(
            inv -> {
              Jwt j = inv.getArgument(0);
              String role = inv.getArgument(1);

              Map<String, Object> realmAccess = j.getClaim("realm_access");
              if (realmAccess == null) return false;
              Object rolesObj = realmAccess.get("roles");
              if (!(rolesObj instanceof List<?> roles)) return false;

              return roles.stream().anyMatch(r -> role.equals(String.valueOf(r)));
            });
  }

  // ==========================================================
  // REGISTER TESTS
  // ==========================================================
  @Nested
  @DisplayName("POST /api/usermanagement/register")
  class RegisterTests {

    @Test
    @DisplayName("400 when role is unsupported")
    void register_unsupportedRole_returns400() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      RegisterUserRequest req = buildRegisterRequest("u1", "Unknown_Role");
      Jwt caller = buildJwt("gov", "Government_Admin");

      mockMvc
          .perform(
              post("/api/usermanagement/register")
                  .with(jwt().jwt(caller))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(req)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName(
        "403 when City_Manager tries to create Cycle_Provider (should be Cycle_Admin only)")
    void register_cycleProvider_byCityManager_forbidden() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      RegisterUserRequest req = buildRegisterRequest("cycleProvider1", "Cycle_Provider");
      Jwt caller = buildJwt("citymgr", "City_Manager");

      mockMvc
          .perform(
              post("/api/usermanagement/register")
                  .with(jwt().jwt(caller))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(req)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("201 when Cycle_Admin creates Cycle_Provider (allowed)")
    void register_cycleProvider_byCycleAdmin_created() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      RegisterUserRequest req = buildRegisterRequest("cycleProvider1", "Cycle_Provider");
      Jwt caller = buildJwt("cycleadmin", "Cycle_Admin");

      RegisterUserResponse resp = Mockito.mock(RegisterUserResponse.class);
      when(userManagementService.registerUser(any(RegisterUserRequest.class))).thenReturn(resp);

      mockMvc
          .perform(
              post("/api/usermanagement/register")
                  .with(jwt().jwt(caller))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(req)))
          .andExpect(status().isCreated());

      verify(userManagementService).registerUser(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("201 when Government_Admin creates City_Manager (allowed)")
    void register_cityManager_byGovAdmin_created() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      RegisterUserRequest req = buildRegisterRequest("citymgr1", "City_Manager");
      Jwt caller = buildJwt("gov", "Government_Admin");

      RegisterUserResponse resp = Mockito.mock(RegisterUserResponse.class);
      when(userManagementService.registerUser(any(RegisterUserRequest.class))).thenReturn(resp);

      mockMvc
          .perform(
              post("/api/usermanagement/register")
                  .with(jwt().jwt(caller))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(req)))
          .andExpect(status().isCreated());

      verify(userManagementService).registerUser(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName(
        "403 when Government_Admin tries to create Bus_Admin (should be City_Manager only)")
    void register_busAdmin_byGovAdmin_forbidden() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      RegisterUserRequest req = buildRegisterRequest("busadmin1", "Bus_Admin");
      Jwt caller = buildJwt("gov", "Government_Admin");

      mockMvc
          .perform(
              post("/api/usermanagement/register")
                  .with(jwt().jwt(caller))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(req)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("201 when City_Manager creates Bus_Admin (allowed)")
    void register_busAdmin_byCityManager_created() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      RegisterUserRequest req = buildRegisterRequest("busadmin1", "Bus_Admin");
      Jwt caller = buildJwt("citymgr", "City_Manager");

      RegisterUserResponse resp = Mockito.mock(RegisterUserResponse.class);
      when(userManagementService.registerUser(any(RegisterUserRequest.class))).thenReturn(resp);

      mockMvc
          .perform(
              post("/api/usermanagement/register")
                  .with(jwt().jwt(caller))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(req)))
          .andExpect(status().isCreated());

      verify(userManagementService).registerUser(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("400 when service throws RuntimeException (bad request)")
    void register_serviceThrows_returns400() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      RegisterUserRequest req = buildRegisterRequest("busadmin1", "Bus_Admin");
      Jwt caller = buildJwt("citymgr", "City_Manager");

      when(userManagementService.registerUser(any(RegisterUserRequest.class)))
          .thenThrow(new RuntimeException("Username already exists"));

      mockMvc
          .perform(
              post("/api/usermanagement/register")
                  .with(jwt().jwt(caller))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(req)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Username already exists"));
    }
  }

  // ==========================================================
  // DELETE TESTS
  // ==========================================================
  @Nested
  @DisplayName("DELETE /api/usermanagement/delete")
  class DeleteTests {

    @Test
    @DisplayName("403 when non-Government_Admin tries to delete")
    void delete_nonGovAdmin_forbidden() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("cycleadmin", "Cycle_Admin");

      mockMvc
          .perform(
              delete("/api/usermanagement/delete")
                  .with(jwt().jwt(caller))
                  .param("username", "someone"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.message", containsString("Only Government_Admin")));
    }

    @Test
    @DisplayName("400 when Government_Admin tries to delete self")
    void delete_selfDelete_badRequest() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("gov", "Government_Admin");

      mockMvc
          .perform(
              delete("/api/usermanagement/delete").with(jwt().jwt(caller)).param("username", "gov"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", containsString("cannot delete your own")));
    }

    @Test
    @DisplayName("200 when Government_Admin deletes another user")
    void delete_govAdmin_success() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("gov", "Government_Admin");

      doNothing().when(userManagementService).deleteUser("u1");

      mockMvc
          .perform(
              delete("/api/usermanagement/delete").with(jwt().jwt(caller)).param("username", "u1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("User deleted successfully"))
          .andExpect(jsonPath("$.username").value("u1"));

      verify(userManagementService).deleteUser("u1");
    }

    @Test
    @DisplayName("400 when delete service throws exception")
    void delete_serviceThrows_returns400() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("gov", "Government_Admin");

      doThrow(new RuntimeException("User not found"))
          .when(userManagementService)
          .deleteUser("missing");

      mockMvc
          .perform(
              delete("/api/usermanagement/delete")
                  .with(jwt().jwt(caller))
                  .param("username", "missing"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("User not found"));
    }
  }

  // ==========================================================
  // LIST USERS TESTS
  // ==========================================================
  @Nested
  @DisplayName("GET /api/usermanagement/users")
  class ListUsersTests {

    @Test
    @DisplayName("403 when non-Government_Admin tries to list users")
    void listUsers_nonGovAdmin_forbidden() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("citymgr", "City_Manager");

      mockMvc
          .perform(get("/api/usermanagement/users").with(jwt().jwt(caller)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.message", containsString("Only Government_Admin")));
    }

    @Test
    @DisplayName("200 when Government_Admin lists users")
    void listUsers_govAdmin_ok() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("gov", "Government_Admin");

      UserRepresentation u1 = new UserRepresentation();
      u1.setUsername("user1");
      UserRepresentation u2 = new UserRepresentation();
      u2.setUsername("user2");

      when(userManagementService.getAllUsers()).thenReturn(List.of(u1, u2));

      mockMvc
          .perform(get("/api/usermanagement/users").with(jwt().jwt(caller)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].username").value("user1"))
          .andExpect(jsonPath("$[1].username").value("user2"));
    }
  }
}
