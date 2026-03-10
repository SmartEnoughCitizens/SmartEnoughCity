package com.trinity.hermes.usermanagement.service;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.notification.services.mail.MailService;
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
import org.springframework.beans.factory.annotation.Qualifier;
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
  private final MailService mailService;

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

  public UserManagementService(
      Keycloak keycloak,
      @Value("${keycloak.realm}") String realm,
      @Qualifier("getMailService") MailService mailService) {
    this.keycloak = keycloak;
    this.realm = realm;
    this.mailService = mailService;
  }

  private RealmResource getRealmResource() {
    return keycloak.realm(realm);
  }

  private UsersResource getUsersResource() {
    return getRealmResource().users();
  }

  public RegisterUserResponse registerUser(RegisterUserRequest request) {

    if (!ALLOWED_ROLES.contains(request.getRole())) {
      throw new IllegalArgumentException("Role not allowed: " + request.getRole());
    }

    List<UserRepresentation> existingUsers =
        getUsersResource().search(request.getUsername(), 0, 10);

    if (existingUsers.stream()
        .map(UserRepresentation::getUsername)
        .anyMatch(request.getUsername()::equals)) {
      throw new RuntimeException("Username already exists: " + request.getUsername());
    }

    UserRepresentation user = new UserRepresentation();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setEnabled(true);
    user.setEmailVerified(true);

    Response response = getUsersResource().create(user);

    if (response.getStatus() != 201) {
      throw new RuntimeException("Failed to create user. Status: " + response.getStatus());
    }

    String userId = CreatedResponseUtil.getCreatedId(response);

    log.info("User created successfully. ID: {}, Username: {}", userId, request.getUsername());

    String tempPassword = generateTempPassword();
    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(tempPassword);
    credential.setTemporary(false);

    getUsersResource().get(userId).resetPassword(credential);

    log.info("Temporary password set for user: {}", request.getUsername());

    assignRole(userId, request.getRole());

    String message = "User registered successfully.";
    try {
      sendWelcomeEmail(
          request.getEmail(), request.getUsername(), request.getFirstName(), tempPassword);
      message = "User registered successfully. A welcome email has been sent.";
    } catch (Exception e) {
      log.error("Failed to send welcome email to {}: {}", request.getEmail(), e.getMessage());
    }

    return new RegisterUserResponse(
        userId, request.getUsername(), request.getEmail(), request.getRole(), message);
  }

  /**
   * Finds a user's Keycloak ID by username.
   *
   * @param username the username to search for
   * @return the Keycloak user ID
   * @throws RuntimeException if the user is not found
   */
  public String findUserIdByUsername(String username) {
    List<UserRepresentation> users = getUsersResource().search(username, 0, 10);
    return users.stream()
        .filter(u -> u.getUsername().equalsIgnoreCase(username))
        .findFirst()
        .map(UserRepresentation::getId)
        .orElseThrow(() -> new RuntimeException("User not found: " + username));
  }

  public void deleteUser(String username) {

    String userId = findUserIdByUsername(username);
    getUsersResource().get(userId).remove();

    log.info("User deleted: {}", LogSanitizer.sanitizeLog(username));
  }

  public List<UserRepresentation> getAllUsers() {
    return getUsersResource().search("", 0, 100);
  }

  /**
   * Returns all users that have the given realm role.
   *
   * @param roleName the Keycloak realm role name
   * @return list of users with that role
   */
  public List<UserRepresentation> getUsersByRole(String roleName) {
    return getRealmResource().roles().get(roleName).getUserMembers(0, 100);
  }

  /**
   * Returns the realm-level role names assigned to a user.
   *
   * @param userId the Keycloak user ID
   * @return set of role names
   */
  public Set<String> getUserRoles(String userId) {
    List<RoleRepresentation> roles =
        getUsersResource().get(userId).roles().realmLevel().listEffective();
    Set<String> roleNames = new HashSet<>();
    for (RoleRepresentation role : roles) {
      roleNames.add(role.getName());
    }
    return roleNames;
  }

  private void assignRole(String userId, String roleName) {

    RoleRepresentation role = getRealmResource().roles().get(roleName).toRepresentation();

    if (role == null) {
      throw new RuntimeException("Role not found in Keycloak: " + roleName);
    }

    getRealmResource()
        .users()
        .get(userId)
        .roles()
        .realmLevel()
        .add(Collections.singletonList(role));

    log.info("Role '{}' assigned to user ID: {}", roleName, userId);
  }

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

  private String generateTempPassword() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "!A1";
  }

  private void sendWelcomeEmail(
      String email, String username, String firstName, String tempPassword) {
    String subject = "You've been invited to SmartEnoughCity Dashboard";
    String html =
        "<div style='font-family: Arial, sans-serif; max-width: 600px;'>"
            + "<h2>Welcome to SmartEnoughCity, "
            + firstName
            + "!</h2>"
            + "<p>Your account has been created. Use the credentials below to log in:</p>"
            + "<table style='border-collapse: collapse; margin: 16px 0;'>"
            + "<tr><td style='padding: 8px; font-weight: bold;'>Username:</td>"
            + "<td style='padding: 8px;'>"
            + username
            + "</td></tr>"
            + "<tr><td style='padding: 8px; font-weight: bold;'>Temporary Password:</td>"
            + "<td style='padding: 8px; font-family: monospace;'>"
            + tempPassword
            + "</td></tr>"
            + "</table>"
            + "<p style='color: #888; font-size: 12px;'>If you did not expect this email, "
            + "please ignore it.</p>"
            + "</div>";
    mailService.sendEmail(email, subject, html, null);
    log.info("Welcome email sent to: {}", email);
  }
}
