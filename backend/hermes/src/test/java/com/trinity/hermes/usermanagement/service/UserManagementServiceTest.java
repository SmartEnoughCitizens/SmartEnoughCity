package com.trinity.hermes.usermanagement.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trinity.hermes.notification.services.mail.MailService;
import com.trinity.hermes.usermanagement.dto.RegisterUserRequest;
import com.trinity.hermes.usermanagement.dto.RegisterUserResponse;
import com.trinity.hermes.usermanagement.entity.PasswordResetTokenEntity;
import com.trinity.hermes.usermanagement.repository.PasswordResetTokenRepository;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserManagementServiceTest {

  @Mock Keycloak keycloak;

  @Mock RealmResource realmResource;
  @Mock UsersResource usersResource;
  @Mock UserResource userResource;

  @Mock RolesResource rolesResource;
  @Mock RoleResource roleResource;

  @Mock RoleMappingResource roleMappingResource;
  @Mock RoleScopeResource roleScopeResource;

  @Mock Response createResponse;

  @Mock MailService mailService;

  @Mock PasswordResetTokenRepository passwordResetTokenRepository;

  UserManagementService service;

  @BeforeEach
  void setup() {

    String realm = "user-management-realm";
    service = new UserManagementService(keycloak, realm, mailService, passwordResetTokenRepository);

    lenient().when(keycloak.realm(realm)).thenReturn(realmResource);
    lenient().when(realmResource.users()).thenReturn(usersResource);
    lenient().when(realmResource.roles()).thenReturn(rolesResource);
    lenient().when(usersResource.get(anyString())).thenReturn(userResource);
    lenient().when(userResource.roles()).thenReturn(roleMappingResource);
    lenient().when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
  }

  private RegisterUserRequest req(String username, String role) {
    RegisterUserRequest r = new RegisterUserRequest();
    r.setUsername(username);
    r.setEmail(username + "@mail.com");
    r.setFirstName("First");
    r.setLastName("Last");
    r.setRole(role);
    return r;
  }

  private UserRepresentation userRep(String id, String username) {
    UserRepresentation u = new UserRepresentation();
    u.setId(id);
    u.setUsername(username);
    return u;
  }

  @Test
  void registerUser_rejectsRoleNotAllowed() {
    RegisterUserRequest request = req("u1", "Government_Admin"); // not in ALLOWED_ROLES

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> service.registerUser(request));

    assertTrue(ex.getMessage().contains("Role not allowed"));
    verifyNoInteractions(usersResource);
  }

  @Test
  void registerUser_throwsWhenUsernameAlreadyExists() {
    RegisterUserRequest request = req("existing", "Cycle_Provider");

    when(usersResource.search(eq("existing"), eq(0), eq(10)))
        .thenReturn(List.of(userRep("id-1", "existing"))); // exact same username exists

    RuntimeException ex = assertThrows(RuntimeException.class, () -> service.registerUser(request));

    assertTrue(ex.getMessage().contains("Username already exists"));
    verify(usersResource, never()).create(any(UserRepresentation.class));
  }

  @Test
  void registerUser_throwsWhenCreateReturnsNon201() {
    RegisterUserRequest request = req("newuser", "Cycle_Provider");

    when(usersResource.search(eq("newuser"), eq(0), eq(10)))
        .thenReturn(List.of()); // no existing users

    when(usersResource.create(any(UserRepresentation.class))).thenReturn(createResponse);
    when(createResponse.getStatus()).thenReturn(400); // fail

    RuntimeException ex = assertThrows(RuntimeException.class, () -> service.registerUser(request));

    assertTrue(ex.getMessage().contains("Failed to create user"));
    verify(usersResource, never()).get(anyString());
  }

  @Test
  void registerUser_success_setsGeneratedPasswordAndAssignsRole() {
    RegisterUserRequest request = req("cycleprov1", "Cycle_Provider");

    when(usersResource.search(eq("cycleprov1"), eq(0), eq(10))).thenReturn(List.of());
    when(usersResource.create(any(UserRepresentation.class))).thenReturn(createResponse);
    when(createResponse.getStatus()).thenReturn(201);

    try (MockedStatic<CreatedResponseUtil> mocked = Mockito.mockStatic(CreatedResponseUtil.class)) {
      mocked.when(() -> CreatedResponseUtil.getCreatedId(createResponse)).thenReturn("userId-123");

      RoleRepresentation roleRep = new RoleRepresentation();
      roleRep.setName("Cycle_Provider");
      when(rolesResource.get("Cycle_Provider")).thenReturn(roleResource);
      when(roleResource.toRepresentation()).thenReturn(roleRep);

      RegisterUserResponse resp = service.registerUser(request);

      assertNotNull(resp);
      assertEquals("userId-123", resp.getUserId());
      assertEquals("cycleprov1", resp.getUsername());
      assertEquals("Cycle_Provider", resp.getRole());

      ArgumentCaptor<CredentialRepresentation> credCaptor =
          ArgumentCaptor.forClass(CredentialRepresentation.class);
      verify(userResource).resetPassword(credCaptor.capture());

      CredentialRepresentation cred = credCaptor.getValue();
      assertEquals(CredentialRepresentation.PASSWORD, cred.getType());
      assertNotNull(cred.getValue());
      assertFalse(cred.getValue().isBlank());
      assertFalse(cred.isTemporary());

      verify(roleScopeResource)
          .add(
              argThat(
                  list ->
                      list != null
                          && list.size() == 1
                          && "Cycle_Provider".equals(list.get(0).getName())));
    }
  }

  @Test
  void registerUser_throwsWhenRoleNotFoundInKeycloak() {
    RegisterUserRequest request = req("u3", "Train_Provider");

    when(usersResource.search(eq("u3"), eq(0), eq(10))).thenReturn(List.of());
    when(usersResource.create(any(UserRepresentation.class))).thenReturn(createResponse);
    when(createResponse.getStatus()).thenReturn(201);

    try (MockedStatic<CreatedResponseUtil> mocked = Mockito.mockStatic(CreatedResponseUtil.class)) {
      mocked.when(() -> CreatedResponseUtil.getCreatedId(createResponse)).thenReturn("userId-333");

      when(rolesResource.get("Train_Provider")).thenReturn(roleResource);
      when(roleResource.toRepresentation()).thenReturn(null); // simulate not found

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> service.registerUser(request));

      assertTrue(ex.getMessage().contains("Role not found in Keycloak"));
      verify(roleScopeResource, never()).add(anyList());
    }
  }

  @Test
  void deleteUser_throwsWhenUserNotFound() {
    when(usersResource.search(eq("missing"), eq(0), eq(10)))
        .thenReturn(List.of(userRep("id1", "other"))); // none matches "missing"

    RuntimeException ex = assertThrows(RuntimeException.class, () -> service.deleteUser("missing"));
    assertTrue(ex.getMessage().contains("User not found"));
    verify(usersResource, never()).get(anyString());
  }

  @Test
  void deleteUser_success_removesUser() {
    UserRepresentation target = userRep("id-99", "ToDelete");

    when(usersResource.search(eq("ToDelete"), eq(0), eq(10))).thenReturn(List.of(target));

    when(usersResource.get("id-99")).thenReturn(userResource);

    service.deleteUser("ToDelete");

    verify(userResource).remove();
  }

  @Test
  void getAllUsers_callsSearchWithEmptyQueryAndLimit100() {
    when(usersResource.search(eq(""), eq(0), eq(100))).thenReturn(List.of());

    service.getAllUsers();

    verify(usersResource).search(eq(""), eq(0), eq(100));
  }

  // ---- Forgot / Reset Password Tests ----

  @Test
  void initiateForgotPassword_savesTokenAndSendsEmail_whenUserFound() {
    UserRepresentation user = userRep("kc-id-1", "testuser");
    user.setEmail("test@example.com");

    when(usersResource.searchByEmail(eq("test@example.com"), eq(true)))
        .thenReturn(List.of(user));

    service.initiateForgotPassword("test@example.com");

    verify(passwordResetTokenRepository).deleteByKeycloakUserId("kc-id-1");
    verify(passwordResetTokenRepository).save(any(PasswordResetTokenEntity.class));
    verify(mailService).sendEmail(eq("test@example.com"), anyString(), anyString(), isNull());
  }

  @Test
  void initiateForgotPassword_doesNothing_whenUserNotFound() {
    when(usersResource.searchByEmail(eq("unknown@example.com"), eq(true)))
        .thenReturn(List.of());

    service.initiateForgotPassword("unknown@example.com");

    verify(passwordResetTokenRepository, never()).save(any());
    verify(mailService, never()).sendEmail(anyString(), anyString(), anyString(), any());
  }

  @Test
  void resetPassword_resetsKeycloakAndDeletesToken_whenTokenValid() {
    PasswordResetTokenEntity token =
        PasswordResetTokenEntity.builder()
            .token("valid-token")
            .keycloakUserId("kc-id-2")
            .expiresAt(LocalDateTime.now().plusHours(1))
            .build();

    when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
    when(usersResource.get("kc-id-2")).thenReturn(userResource);

    service.resetPassword("valid-token", "NewPassword1!");

    ArgumentCaptor<CredentialRepresentation> credCaptor =
        ArgumentCaptor.forClass(CredentialRepresentation.class);
    verify(userResource).resetPassword(credCaptor.capture());
    assertEquals("NewPassword1!", credCaptor.getValue().getValue());
    assertFalse(credCaptor.getValue().isTemporary());

    verify(passwordResetTokenRepository).deleteByToken("valid-token");
  }

  @Test
  void resetPassword_throwsForUnknownToken() {
    when(passwordResetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> service.resetPassword("bad-token", "pass"));

    assertTrue(ex.getMessage().contains("Invalid or expired token"));
    verifyNoInteractions(userResource);
  }

  @Test
  void resetPassword_throwsForExpiredToken() {
    PasswordResetTokenEntity expiredToken =
        PasswordResetTokenEntity.builder()
            .token("expired-token")
            .keycloakUserId("kc-id-3")
            .expiresAt(LocalDateTime.now().minusHours(2))
            .build();

    when(passwordResetTokenRepository.findByToken("expired-token"))
        .thenReturn(Optional.of(expiredToken));

    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> service.resetPassword("expired-token", "pass"));

    assertTrue(ex.getMessage().contains("expired"));
    verify(userResource, never()).resetPassword(any());
  }
}
