package com.trinity.hermes.usermanagement.service;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.usermanagement.dto.RegisterUserRequest;
import com.trinity.hermes.usermanagement.dto.RegisterUserResponse;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * All Keycloak Admin API calls happen here.
 *
 * <p>How it works: 1. Keycloak bean (from KeycloakConfig) already has an admin token. 2. We get a
 * RealmResource for "user-management-realm". 3. We call UsersResource to create / delete users. 4.
 * We call RolesResource to assign a realm role to the new user.
 */
@Service
public class UserManagementService {

  private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);

  private final Keycloak keycloak;
  private final String realm;

  // These are the only roles gov_admin is allowed to assign
  private static final Set<String> ALLOWED_ROLES =
      Set.of(
          "City_Manager",
          "Bus_Admin",
          "Bus_Provider",
          "Cycle_Admin",
          "Cycle_Provider",
          "Train_Admin",
          "Train_Provider",
          "Tram_Admin",
          "Tram_Provider");

  public UserManagementService(Keycloak keycloak, @Value("${keycloak.realm}") String realm) {
    this.keycloak = keycloak;
    this.realm = realm;
  }

  // ---------------------------------------------------------------
  // GET REALM RESOURCE (helper)
  // ---------------------------------------------------------------
  private RealmResource getRealmResource() {
    return keycloak.realm(realm);
  }

  private UsersResource getUsersResource() {
    return getRealmResource().users();
  }

  // ---------------------------------------------------------------
  // 1. REGISTER USER
  //    - Creates user in Keycloak
  //    - Sets password
  //    - Assigns the requested role
  // ---------------------------------------------------------------
  public RegisterUserResponse registerUser(RegisterUserRequest request) {

    // --- Safety check: block disallowed roles ---
    if (!ALLOWED_ROLES.contains(request.getRole())) {
      throw new IllegalArgumentException("Role not allowed: " + request.getRole());
    }

    // --- Check if username already exists ---
    List<UserRepresentation> existingUsers =
        getUsersResource().search(request.getUsername(), 0, 10);

    if (existingUsers.stream()
        .map(UserRepresentation::getUsername)
        .anyMatch(request.getUsername()::equals)) {
      throw new RuntimeException("Username already exists: " + request.getUsername());
    }

    // --- Build user representation ---
    UserRepresentation user = new UserRepresentation();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setEnabled(true);
    user.setEmailVerified(true);

    // --- Create user in Keycloak ---
    // Response contains the new user's ID in the Location header
    Response response = getUsersResource().create(user);

    if (response.getStatus() != 201) {
      throw new RuntimeException("Failed to create user. Status: " + response.getStatus());
    }

    // Extract the user ID from the Location header
    // Location header looks like: http://localhost:8081/admin/realms/.../users/{userId}
    String userId = CreatedResponseUtil.getCreatedId(response);

    log.info("User created successfully. ID: {}, Username: {}", userId, request.getUsername());

    // --- Set password ---
    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);

    if (request.getPassword() != null && !request.getPassword().isBlank()) {
      credential.setValue(request.getPassword());
      credential.setTemporary(false); // user does NOT need to change password
    } else {
      credential.setValue("ChangeMe@123"); // default temp password
      credential.setTemporary(true); // user MUST change on first login
    }

    getUsersResource().get(userId).resetPassword(credential);
    log.info("Password set for user: {}", request.getUsername());

    // --- Assign role ---
    assignRole(userId, request.getRole());

    // --- Build and return response ---
    return new RegisterUserResponse(
        userId,
        request.getUsername(),
        request.getEmail(),
        request.getRole(),
        "User registered successfully");
  }

  // ---------------------------------------------------------------
  // 2. DELETE USER
  //    - Deletes user from Keycloak by username
  // ---------------------------------------------------------------
  public void deleteUser(String username) {

    // Find user by username
    List<UserRepresentation> users = getUsersResource().search(username, 0, 10);

    UserRepresentation targetUser =
        users.stream()
            .filter(u -> u.getUsername().equalsIgnoreCase(username))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("User not found: " + username));

    // Delete the user
    getUsersResource().get(targetUser.getId()).remove();

    log.info("User deleted: {}", LogSanitizer.sanitizeLog(username));
  }

  // ---------------------------------------------------------------
  // 3. LIST ALL USERS (bonus -- useful for the UI)
  // ---------------------------------------------------------------
  public List<UserRepresentation> getAllUsers() {
    return getUsersResource().search("", 0, 100);
  }

  // ---------------------------------------------------------------
  // PRIVATE: Assign a realm role to a user
  // ---------------------------------------------------------------
  private void assignRole(String userId, String roleName) {
    // Get the role object from Keycloak by name
    RoleRepresentation role = getRealmResource().roles().get(roleName).toRepresentation();

    if (role == null) {
      throw new RuntimeException("Role not found in Keycloak: " + roleName);
    }

    // Assign that role to the user
    getRealmResource()
        .users()
        .get(userId)
        .roles()
        .realmLevel()
        .add(Collections.singletonList(role));

    log.info("Role '{}' assigned to user ID: {}", roleName, userId);
  }

  /**
   * Keycloak puts realm roles inside the JWT token like this:
   *
   * <p>{ "realm_access": { "roles": ["Government_Admin", "offline_access"] } }
   *
   * <p>This method reads that and checks if the required role is present.
   */
  public boolean hasRole(Jwt jwt, String requiredRole) {
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

    if (realmAccess == null) {
      return false;
    }

    List<String> roles = (List<String>) realmAccess.get("roles");

    if (roles == null) {
      return false;
    }

    return roles.contains(requiredRole);
  }
}
