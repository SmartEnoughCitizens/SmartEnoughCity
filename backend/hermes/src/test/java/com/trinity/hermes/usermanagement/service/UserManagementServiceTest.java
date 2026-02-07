package com.trinity.hermes.usermanagement.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trinity.hermes.usermanagement.dto.RegisterUserRequest;
import com.trinity.hermes.usermanagement.dto.RegisterUserResponse;
import jakarta.ws.rs.core.Response;
import java.util.List;
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

  // Keycloak resource chain
  @Mock RealmResource realmResource;
  @Mock UsersResource usersResource;
  @Mock UserResource userResource;

  // role assignment chain
  @Mock RolesResource rolesResource;
  @Mock RoleResource roleResource;

  @Mock RoleMappingResource roleMappingResource;
  @Mock RoleScopeResource roleScopeResource;

  @Mock Response createResponse;

  UserManagementService service;

  private final String realm = "user-management-realm";

  @BeforeEach
  void setup() {
    service = new UserManagementService(keycloak, realm);

    // keycloak.realm(realm) -> realmResource
    lenient().when(keycloak.realm(realm)).thenReturn(realmResource);

    // realmResource.users() -> usersResource
    lenient().when(realmResource.users()).thenReturn(usersResource);

    // realmResource.roles() -> rolesResource
    lenient().when(realmResource.roles()).thenReturn(rolesResource);

    // usersResource.get(userId) -> userResource
    lenient().when(usersResource.get(anyString())).thenReturn(userResource);

    // userResource.roles().realmLevel().add(...)
    lenient().when(userResource.roles()).thenReturn(roleMappingResource);
    lenient().when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
  }

  // ------------------------
  // Helpers
  // ------------------------
  private RegisterUserRequest req(String username, String role, String password) {
    RegisterUserRequest r = new RegisterUserRequest();
    r.setUsername(username);
    r.setEmail(username + "@mail.com");
    r.setFirstName("First");
    r.setLastName("Last");
    r.setRole(role);
    r.setPassword(password);
    return r;
  }

  private UserRepresentation userRep(String id, String username) {
    UserRepresentation u = new UserRepresentation();
    u.setId(id);
    u.setUsername(username);
    return u;
  }

  // ==========================================================
  // registerUser tests
  // ==========================================================

  @Test
  void registerUser_rejectsRoleNotAllowed() {
    RegisterUserRequest request = req("u1", "Government_Admin", "x"); // not in ALLOWED_ROLES

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> service.registerUser(request));

    assertTrue(ex.getMessage().contains("Role not allowed"));
    verifyNoInteractions(usersResource); // should fail before calling Keycloak
  }

  @Test
  void registerUser_throwsWhenUsernameAlreadyExists() {
    RegisterUserRequest request = req("existing", "Cycle_Provider", "Pass@123");

    when(usersResource.search(eq("existing"), eq(0), eq(10)))
        .thenReturn(List.of(userRep("id-1", "existing"))); // exact same username exists

    RuntimeException ex = assertThrows(RuntimeException.class, () -> service.registerUser(request));

    assertTrue(ex.getMessage().contains("Username already exists"));
    verify(usersResource, never()).create(any(UserRepresentation.class));
  }

  @Test
  void registerUser_throwsWhenCreateReturnsNon201() {
    RegisterUserRequest request = req("newuser", "Cycle_Provider", "Pass@123");

    when(usersResource.search(eq("newuser"), eq(0), eq(10)))
        .thenReturn(List.of()); // no existing users

    when(usersResource.create(any(UserRepresentation.class))).thenReturn(createResponse);
    when(createResponse.getStatus()).thenReturn(400); // fail

    RuntimeException ex = assertThrows(RuntimeException.class, () -> service.registerUser(request));

    assertTrue(ex.getMessage().contains("Failed to create user"));
    verify(usersResource, never()).get(anyString()); // no password reset if create failed
  }

  @Test
  void registerUser_success_withProvidedPassword_setsPermanentPassword_andAssignsRole() {
    RegisterUserRequest request = req("cycleprov1", "Cycle_Provider", "Pass@123");

    when(usersResource.search(eq("cycleprov1"), eq(0), eq(10))).thenReturn(List.of());
    when(usersResource.create(any(UserRepresentation.class))).thenReturn(createResponse);
    when(createResponse.getStatus()).thenReturn(201);

    // mock CreatedResponseUtil.getCreatedId(response) (static)
    try (MockedStatic<CreatedResponseUtil> mocked = Mockito.mockStatic(CreatedResponseUtil.class)) {
      mocked.when(() -> CreatedResponseUtil.getCreatedId(createResponse)).thenReturn("userId-123");

      // role exists
      RoleRepresentation roleRep = new RoleRepresentation();
      roleRep.setName("Cycle_Provider");
      when(rolesResource.get("Cycle_Provider")).thenReturn(roleResource);
      when(roleResource.toRepresentation()).thenReturn(roleRep);

      RegisterUserResponse resp = service.registerUser(request);

      assertNotNull(resp);
      assertEquals("userId-123", resp.getUserId());
      assertEquals("cycleprov1", resp.getUsername());
      assertEquals("Cycle_Provider", resp.getRole());

      // verify password reset called with non-temporary credential and correct password
      ArgumentCaptor<CredentialRepresentation> credCaptor =
          ArgumentCaptor.forClass(CredentialRepresentation.class);
      verify(userResource).resetPassword(credCaptor.capture());

      CredentialRepresentation cred = credCaptor.getValue();
      assertEquals(CredentialRepresentation.PASSWORD, cred.getType());
      assertEquals("Pass@123", cred.getValue());
      assertFalse(cred.isTemporary());

      // verify role assignment happens
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
  void registerUser_success_withBlankPassword_setsDefaultTemporaryPassword() {
    RegisterUserRequest request =
        req("u2", "Bus_Provider", "   "); // blank => default ChangeMe@123, temporary=true

    when(usersResource.search(eq("u2"), eq(0), eq(10))).thenReturn(List.of());
    when(usersResource.create(any(UserRepresentation.class))).thenReturn(createResponse);
    when(createResponse.getStatus()).thenReturn(201);

    try (MockedStatic<CreatedResponseUtil> mocked = Mockito.mockStatic(CreatedResponseUtil.class)) {
      mocked.when(() -> CreatedResponseUtil.getCreatedId(createResponse)).thenReturn("userId-222");

      RoleRepresentation roleRep = new RoleRepresentation();
      roleRep.setName("Bus_Provider");
      when(rolesResource.get("Bus_Provider")).thenReturn(roleResource);
      when(roleResource.toRepresentation()).thenReturn(roleRep);

      service.registerUser(request);

      ArgumentCaptor<CredentialRepresentation> credCaptor =
          ArgumentCaptor.forClass(CredentialRepresentation.class);
      verify(userResource).resetPassword(credCaptor.capture());

      CredentialRepresentation cred = credCaptor.getValue();
      assertEquals("ChangeMe@123", cred.getValue());
      assertTrue(cred.isTemporary());
    }
  }

  @Test
  void registerUser_throwsWhenRoleNotFoundInKeycloak() {
    RegisterUserRequest request = req("u3", "Train_Provider", "Pass@123");

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

  // ==========================================================
  // deleteUser tests
  // ==========================================================

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

  // ==========================================================
  // getAllUsers tests
  // ==========================================================

  @Test
  void getAllUsers_callsSearchWithEmptyQueryAndLimit100() {
    when(usersResource.search(eq(""), eq(0), eq(100))).thenReturn(List.of());

    service.getAllUsers();

    verify(usersResource).search(eq(""), eq(0), eq(100));
  }
}
