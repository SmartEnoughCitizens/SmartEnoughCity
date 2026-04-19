package com.trinity.hermes.usermanagement.service;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.notification.services.mail.MailService;
import com.trinity.hermes.usermanagement.dto.ChangePasswordRequest;
import com.trinity.hermes.usermanagement.dto.ProfileResponse;
import com.trinity.hermes.usermanagement.dto.RegisterUserRequest;
import com.trinity.hermes.usermanagement.dto.RegisterUserResponse;
import com.trinity.hermes.usermanagement.dto.UpdateProfileRequest;
import com.trinity.hermes.usermanagement.entity.PasswordResetTokenEntity;
import com.trinity.hermes.usermanagement.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${keycloak.server-url}")
  private String keycloakServerUrl;

  @Value("${keycloak.client-id}")
  private String clientId;

  @Value("${keycloak.client-secret}")
  private String clientSecret;

  @Value("${app.frontend-url}")
  private String frontendUrl;

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
      @Qualifier("getMailService") MailService mailService,
      PasswordResetTokenRepository passwordResetTokenRepository) {
    this.keycloak = keycloak;
    this.realm = realm;
    this.mailService = mailService;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
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
      throw new RuntimeException("Username is already taken.");
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
      String msg =
          switch (response.getStatus()) {
            case 400 -> "Invalid username. Usernames cannot contain spaces or special characters.";
            case 409 -> "An account with this email address already exists.";
            default -> "Failed to create user (status " + response.getStatus() + ").";
          };
      throw new RuntimeException(msg);
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

  public String getUserEmail(String userId) {
    return getUsersResource().search(userId, 0, 10).stream()
        .filter(u -> u.getUsername().equalsIgnoreCase(userId) || u.getId().equals(userId))
        .findFirst()
        .map(UserRepresentation::getEmail)
        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
  }

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

  public ProfileResponse getProfile(String username) {
    String userId = findUserIdByUsername(username);
    UserRepresentation user = getUsersResource().get(userId).toRepresentation();
    return new ProfileResponse(
        user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName());
  }

  public void updateProfile(String username, UpdateProfileRequest request) {
    String userId = findUserIdByUsername(username);
    UserRepresentation user = getUsersResource().get(userId).toRepresentation();
    user.setEmail(request.getEmail());
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    getUsersResource().get(userId).update(user);
    log.info("Profile updated for user: {}", LogSanitizer.sanitizeLog(username));
  }

  public void changePassword(String username, ChangePasswordRequest request) {
    // Verify current password by re-authenticating against Keycloak
    String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", clientId);
    body.add("client_secret", clientSecret);
    body.add("grant_type", "password");
    body.add("username", username);
    body.add("password", request.getCurrentPassword());

    HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(body, headers);

    try {
      restTemplate.postForEntity(tokenUrl, tokenRequest, Map.class);
    } catch (HttpClientErrorException e) {
      throw new RuntimeException("Current password is incorrect");
    }

    // Reset password via Keycloak Admin API
    String userId = findUserIdByUsername(username);
    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(request.getNewPassword());
    credential.setTemporary(false);
    getUsersResource().get(userId).resetPassword(credential);

    log.info("Password changed for user: {}", LogSanitizer.sanitizeLog(username));
  }

  @Transactional
  public void initiateForgotPassword(String email) {
    List<UserRepresentation> users = getUsersResource().searchByEmail(email, true);
    if (!users.isEmpty()) {
      UserRepresentation user = users.get(0);
      String keycloakUserId = user.getId();

      passwordResetTokenRepository.deleteByKeycloakUserId(keycloakUserId);

      String token = UUID.randomUUID().toString();
      LocalDateTime expiresAt = LocalDateTime.now(ZoneId.of("Europe/Dublin")).plusHours(1);

      PasswordResetTokenEntity resetToken =
          PasswordResetTokenEntity.builder()
              .token(token)
              .keycloakUserId(keycloakUserId)
              .expiresAt(expiresAt)
              .build();
      passwordResetTokenRepository.save(resetToken);

      try {
        sendPasswordResetEmail(email, token);
      } catch (Exception e) {
        log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
      }
    }
    // Always return normally — never reveal whether the email exists
  }

  @Transactional
  public void resetPassword(String token, String newPassword) {
    PasswordResetTokenEntity resetToken =
        passwordResetTokenRepository
            .findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

    if (resetToken.getExpiresAt().isBefore(LocalDateTime.now(ZoneId.of("Europe/Dublin")))) {
      throw new RuntimeException("Token has expired");
    }

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(newPassword);
    credential.setTemporary(false);
    getUsersResource().get(resetToken.getKeycloakUserId()).resetPassword(credential);

    passwordResetTokenRepository.deleteByToken(token);

    log.info("Password reset for Keycloak user: {}", resetToken.getKeycloakUserId());
  }

  private String generateTempPassword() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "!A1";
  }

  private void sendPasswordResetEmail(String email, String token) {
    String resetLink = frontendUrl + "/reset-password?token=" + token;
    String subject = "Reset your SmartEnoughCity password";
    String html =
        "<div style='font-family: Arial, sans-serif; max-width: 600px;'>"
            + "<h2>Password Reset Request</h2>"
            + "<p>We received a request to reset your SmartEnoughCity password.</p>"
            + "<p>Click the button below to set a new password. This link expires in 1 hour.</p>"
            + "<a href='"
            + resetLink
            + "' style='display: inline-block; margin: 16px 0; padding: 12px 24px;"
            + " background-color: #1976d2; color: #fff; text-decoration: none;"
            + " border-radius: 4px; font-weight: bold;'>Reset Password</a>"
            + "<p>If you did not request a password reset, you can safely ignore this email.</p>"
            + "<p style='color: #888; font-size: 12px;'>This link will expire in 1 hour.</p>"
            + "</div>";
    mailService.sendEmail(email, subject, html, null);
    log.info("Password reset email sent to: {}", email);
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
            + "<tr><td style='padding: 8px; font-weight: bold;'>Password:</td>"
            + "<td style='padding: 8px; font-family: monospace;'>"
            + tempPassword
            + "</td></tr>"
            + "</table>"
            + "<p>You can change your password at any time from your profile settings.</p>"
            + "<a href='"
            + frontendUrl
            + "' style='display: inline-block; margin: 16px 0; padding: 12px 24px;"
            + " background-color: #1a73e8; color: #ffffff; text-decoration: none;"
            + " border-radius: 4px; font-weight: bold;'>Go to Dashboard</a>"
            + "<p style='color: #888; font-size: 12px;'>If you did not expect this email, "
            + "please ignore it.</p>"
            + "</div>";
    mailService.sendEmail(email, subject, html, null);
    log.info("Welcome email sent to: {}", email);
  }
}
