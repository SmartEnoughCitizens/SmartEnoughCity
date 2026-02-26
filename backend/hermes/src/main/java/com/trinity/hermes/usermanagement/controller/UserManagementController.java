package com.trinity.hermes.usermanagement.controller;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.usermanagement.dto.RegisterUserRequest;
import com.trinity.hermes.usermanagement.dto.RegisterUserResponse;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints: POST /api/usermanagement/register -- create a new user DELETE
 * /api/usermanagement/delete?username={username} -- delete a user GET /api/usermanagement/users --
 * list manageable users
 *
 * <p>How security works here: - @AuthenticationPrincipal Jwt jwt --> Spring injects the decoded JWT
 * token that the frontend sent in the Authorization header. - We extract "realm_access.roles" from
 * that token to check role-based permissions using CREATE_PERMISSIONS and MANAGE_PERMISSIONS.
 */
@RestController
@RequestMapping("/api/usermanagement")
@CrossOrigin(origins = "*")
public class UserManagementController {

  private static final Logger log = LoggerFactory.getLogger(UserManagementController.class);

  private final UserManagementService userManagementService;

  /** Maps target role → set of caller roles allowed to create it. */
  private static final Map<String, Set<String>> CREATE_PERMISSIONS =
      Map.of(
          "Cycle_Admin", Set.of("City_Manager"),
          "Bus_Admin", Set.of("City_Manager"),
          "Train_Admin", Set.of("City_Manager"),
          "Tram_Admin", Set.of("City_Manager"),
          "Cycle_Provider", Set.of("Cycle_Admin"),
          "Bus_Provider", Set.of("Bus_Admin"),
          "Train_Provider", Set.of("Train_Admin"),
          "Tram_Provider", Set.of("Tram_Admin"),
          "City_Manager", Set.of("Government_Admin"));

  /**
   * Ordered list of caller roles from highest to lowest priority. Keycloak composite roles may add
   * sub-roles to the JWT (e.g. City_Manager also contains Bus_Admin), so we match the
   * highest-priority role first.
   */
  private static final List<String> ROLE_PRIORITY =
      List.of(
          "Government_Admin",
          "City_Manager",
          "Bus_Admin",
          "Cycle_Admin",
          "Train_Admin",
          "Tram_Admin");

  /** Maps caller role → set of target roles they can manage (list/delete). */
  private static final Map<String, Set<String>> MANAGE_PERMISSIONS =
      Map.of(
          "Government_Admin", Set.of("City_Manager"),
          "City_Manager", Set.of("Bus_Admin", "Cycle_Admin", "Train_Admin", "Tram_Admin"),
          "Bus_Admin", Set.of("Bus_Provider"),
          "Cycle_Admin", Set.of("Cycle_Provider"),
          "Train_Admin", Set.of("Train_Provider"),
          "Tram_Admin", Set.of("Tram_Provider"));

  @SuppressFBWarnings(
      value = "EI2",
      justification = "Spring-managed dependency injected via constructor; stored in final field.")
  public UserManagementController(UserManagementService userManagementService) {
    this.userManagementService = userManagementService;
  }

  /**
   * Registers a new user based on role-based permissions.
   *
   * @param request user registration details
   * @param jwt authenticated JWT of the caller
   * @return response containing created user details or an error message
   */
  @PostMapping("/register")
  public ResponseEntity<?> registerUser(
      @Valid @RequestBody RegisterUserRequest request, @AuthenticationPrincipal Jwt jwt) {

    log.info("Received request to register user {}", LogSanitizer.sanitizeLog(request));

    Set<String> allowedCreators = CREATE_PERMISSIONS.get(request.getRole());

    if (allowedCreators == null) {
      return ResponseEntity.status(400)
          .body(Map.of("message", "Unsupported role: " + request.getRole()));
    }

    boolean allowed = allowedCreators.stream().anyMatch(r -> userManagementService.hasRole(jwt, r));

    if (!allowed) {
      return ResponseEntity.status(403)
          .body(
              Map.of(
                  "message",
                  "Access denied. Allowed roles to create '"
                      + request.getRole()
                      + "': "
                      + allowedCreators));
    }

    try {
      RegisterUserResponse response = userManagementService.registerUser(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (RuntimeException e) {
      log.error("Error registering user: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  /**
   * Deletes a user by username. The caller must have a role that allows managing the target user's
   * role.
   *
   * @param username username of the user to be deleted
   * @param jwt authenticated JWT of the caller
   * @return success or error response
   */
  @DeleteMapping("/delete")
  public ResponseEntity<?> deleteUser(
      @RequestParam String username, @AuthenticationPrincipal Jwt jwt) {

    Set<String> manageableRoles = getManageableRoles(jwt);

    if (manageableRoles.isEmpty()) {
      return ResponseEntity.status(403)
          .body(Map.of("message", "Access denied. You do not have permission to delete users."));
    }

    try {
      // Verify the target user belongs to a role the caller can manage
      String targetUserId = userManagementService.findUserIdByUsername(username);
      Set<String> targetRoles = userManagementService.getUserRoles(targetUserId);

      boolean canDelete = targetRoles.stream().anyMatch(manageableRoles::contains);
      if (!canDelete) {
        return ResponseEntity.status(403)
            .body(Map.of("message", "Access denied. You cannot delete this user."));
      }

      userManagementService.deleteUser(username);
      return ResponseEntity.ok(
          Map.of("message", "User deleted successfully", "username", username));
    } catch (RuntimeException e) {
      log.error("Error deleting user: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  /**
   * Returns the set of roles that the caller can manage, using ROLE_PRIORITY to pick the
   * highest-level role first (avoids issues with Keycloak composite sub-roles).
   *
   * @param jwt the caller's JWT
   * @return set of manageable role names, empty if none
   */
  private Set<String> getManageableRoles(Jwt jwt) {
    for (String role : ROLE_PRIORITY) {
      if (userManagementService.hasRole(jwt, role) && MANAGE_PERMISSIONS.containsKey(role)) {
        return MANAGE_PERMISSIONS.get(role);
      }
    }
    return Set.of();
  }

  /**
   * Retrieves users that the caller is allowed to manage based on their role.
   *
   * @param jwt authenticated JWT of the caller
   * @return list of manageable users or an error response
   */
  @GetMapping("/users")
  public ResponseEntity<?> getManageableUsers(@AuthenticationPrincipal Jwt jwt) {

    Set<String> manageableRoles = getManageableRoles(jwt);

    if (manageableRoles.isEmpty()) {
      return ResponseEntity.status(403)
          .body(Map.of("message", "Access denied. You do not have permission to manage users."));
    }

    List<UserRepresentation> users = new ArrayList<>();
    for (String role : manageableRoles) {
      try {
        users.addAll(userManagementService.getUsersByRole(role));
      } catch (RuntimeException e) {
        log.warn("Could not fetch users for role {}: {}", role, e.getMessage());
      }
    }
    return ResponseEntity.ok(users);
  }
}
