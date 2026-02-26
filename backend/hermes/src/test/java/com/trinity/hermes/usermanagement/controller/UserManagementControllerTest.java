package com.trinity.hermes.usermanagement.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.hermes.usermanagement.common.TestUtils;
import com.trinity.hermes.usermanagement.dto.RegisterUserRequest;
import com.trinity.hermes.usermanagement.dto.RegisterUserResponse;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      return http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
          .build();
    }

    @Bean
    JwtDecoder jwtDecoder() {

      return token -> {
        throw new JwtException("Not used in tests");
      };
    }
  }

  @BeforeEach
  void resetMocks() {
    Mockito.reset(userManagementService);
  }

  private Jwt buildJwt(String preferredUsername, String... realmRoles) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("preferred_username", preferredUsername)
        .claim("realm_access", Map.of("roles", List.of(realmRoles)))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  private RegisterUserRequest buildRegisterRequest(String username, String role) {
    RegisterUserRequest req = new RegisterUserRequest();
    req.setUsername(username);
    req.setFirstName("Test");
    req.setLastName("User");
    req.setRole(role);
    req.setEmail(username + "@mail.com");
    req.setPassword(TestUtils.randomPassword());
    return req;
  }

  private void mockHasRoleBasedOnJwtClaims() {
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

  @Nested
  @DisplayName("DELETE /api/usermanagement/delete")
  class DeleteTests {

    @Test
    @DisplayName("403 when Bus_Provider (no manage permissions) tries to delete")
    void delete_busProvider_forbidden() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("provider1", "Bus_Provider");

      mockMvc
          .perform(
              delete("/api/usermanagement/delete")
                  .with(jwt().jwt(caller))
                  .param("username", "someone"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.message", containsString("do not have permission to delete")));
    }

    @Test
    @DisplayName("200 when Bus_Admin deletes Bus_Provider (allowed)")
    void delete_busProvider_byBusAdmin_success() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("busadmin", "Bus_Admin");

      // Mock findUserIdByUsername
      when(userManagementService.findUserIdByUsername("bus_provider_1")).thenReturn("user-id-123");

      // Mock getUserRoles to return Bus_Provider role
      when(userManagementService.getUserRoles("user-id-123")).thenReturn(Set.of("Bus_Provider"));

      // Mock deleteUser
      doNothing().when(userManagementService).deleteUser("bus_provider_1");

      mockMvc
          .perform(
              delete("/api/usermanagement/delete")
                  .with(jwt().jwt(caller))
                  .param("username", "bus_provider_1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("User deleted successfully"))
          .andExpect(jsonPath("$.username").value("bus_provider_1"));

      verify(userManagementService).findUserIdByUsername("bus_provider_1");
      verify(userManagementService).getUserRoles("user-id-123");
      verify(userManagementService).deleteUser("bus_provider_1");
    }

    @Test
    @DisplayName("403 when Bus_Admin tries to delete Cycle_Provider (not allowed)")
    void delete_cycleProvider_byBusAdmin_forbidden() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("busadmin", "Bus_Admin");

      // Mock findUserIdByUsername
      when(userManagementService.findUserIdByUsername("cycle_provider_1"))
          .thenReturn("user-id-456");

      // Mock getUserRoles to return Cycle_Provider role (not manageable by Bus_Admin)
      when(userManagementService.getUserRoles("user-id-456")).thenReturn(Set.of("Cycle_Provider"));

      mockMvc
          .perform(
              delete("/api/usermanagement/delete")
                  .with(jwt().jwt(caller))
                  .param("username", "cycle_provider_1"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.message", containsString("You cannot delete this user")));

      verify(userManagementService).findUserIdByUsername("cycle_provider_1");
      verify(userManagementService).getUserRoles("user-id-456");
      verify(userManagementService, never()).deleteUser(anyString());
    }

    @Test
    @DisplayName("200 when City_Manager deletes Bus_Admin (allowed)")
    void delete_busAdmin_byCityManager_success() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("citymgr", "City_Manager");

      when(userManagementService.findUserIdByUsername("bus_admin_1")).thenReturn("user-id-789");
      when(userManagementService.getUserRoles("user-id-789")).thenReturn(Set.of("Bus_Admin"));
      doNothing().when(userManagementService).deleteUser("bus_admin_1");

      mockMvc
          .perform(
              delete("/api/usermanagement/delete")
                  .with(jwt().jwt(caller))
                  .param("username", "bus_admin_1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("User deleted successfully"));

      verify(userManagementService).deleteUser("bus_admin_1");
    }

    @Test
    @DisplayName("400 when findUserIdByUsername throws exception (user not found)")
    void delete_userNotFound_returns400() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("busadmin", "Bus_Admin");

      when(userManagementService.findUserIdByUsername("missing"))
          .thenThrow(new RuntimeException("User not found: missing"));

      mockMvc
          .perform(
              delete("/api/usermanagement/delete")
                  .with(jwt().jwt(caller))
                  .param("username", "missing"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("User not found: missing"));

      verify(userManagementService, never()).deleteUser(anyString());
    }
  }

  @Nested
  @DisplayName("GET /api/usermanagement/users")
  class ListUsersTests {

    @Test
    @DisplayName("403 when Bus_Provider (no manage permissions) tries to list users")
    void listUsers_busProvider_forbidden() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("provider1", "Bus_Provider");

      mockMvc
          .perform(get("/api/usermanagement/users").with(jwt().jwt(caller)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.message", containsString("do not have permission to manage")));
    }

    @Test
    @DisplayName("200 when Bus_Admin lists users (returns Bus_Providers)")
    void listUsers_busAdmin_returnsBusProviders() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("busadmin", "Bus_Admin");

      UserRepresentation u1 = new UserRepresentation();
      u1.setUsername("bus_provider_1");
      u1.setEmail("bp1@example.com");

      UserRepresentation u2 = new UserRepresentation();
      u2.setUsername("bus_provider_2");
      u2.setEmail("bp2@example.com");

      when(userManagementService.getUsersByRole("Bus_Provider")).thenReturn(List.of(u1, u2));

      mockMvc
          .perform(get("/api/usermanagement/users").with(jwt().jwt(caller)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].username").value("bus_provider_1"))
          .andExpect(jsonPath("$[1].username").value("bus_provider_2"));

      verify(userManagementService).getUsersByRole("Bus_Provider");
    }

    @Test
    @DisplayName("200 when City_Manager lists users (returns all admins)")
    void listUsers_cityManager_returnsAdmins() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("citymgr", "City_Manager");

      UserRepresentation busAdmin = new UserRepresentation();
      busAdmin.setUsername("bus_admin_1");

      UserRepresentation cycleAdmin = new UserRepresentation();
      cycleAdmin.setUsername("cycle_admin_1");

      when(userManagementService.getUsersByRole("Bus_Admin")).thenReturn(List.of(busAdmin));
      when(userManagementService.getUsersByRole("Cycle_Admin")).thenReturn(List.of(cycleAdmin));
      when(userManagementService.getUsersByRole("Train_Admin")).thenReturn(List.of());
      when(userManagementService.getUsersByRole("Tram_Admin")).thenReturn(List.of());

      mockMvc
          .perform(get("/api/usermanagement/users").with(jwt().jwt(caller)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(
              jsonPath("$[*].username")
                  .value(org.hamcrest.Matchers.hasItems("bus_admin_1", "cycle_admin_1")));

      verify(userManagementService).getUsersByRole("Bus_Admin");
      verify(userManagementService).getUsersByRole("Cycle_Admin");
      verify(userManagementService).getUsersByRole("Train_Admin");
      verify(userManagementService).getUsersByRole("Tram_Admin");
    }

    @Test
    @DisplayName("500 returns error when getUsersByRole throws exception")
    void listUsers_getUsersByRoleThrows_returnsInternalServerError() throws Exception {
      mockHasRoleBasedOnJwtClaims();

      Jwt caller = buildJwt("busadmin", "Bus_Admin");

      when(userManagementService.getUsersByRole("Bus_Provider"))
          .thenThrow(new RuntimeException("Role not found"));

      mockMvc
          .perform(get("/api/usermanagement/users").with(jwt().jwt(caller)))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.message").value("Failed to fetch users for role: Bus_Provider"));
    }
  }
}
