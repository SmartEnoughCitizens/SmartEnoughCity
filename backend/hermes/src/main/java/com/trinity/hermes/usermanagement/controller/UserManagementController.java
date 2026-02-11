package com.trinity.hermes.usermanagement.controller;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.usermanagement.dto.RegisterUserRequest;
import com.trinity.hermes.usermanagement.dto.RegisterUserResponse;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
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
 * Endpoints: POST /api/usermanagement/register -- create a new user (gov_admin only) DELETE
 * /api/usermanagement/users/{username} -- delete a user (gov_admin only) GET
 * /api/usermanagement/users -- list all users (gov_admin only)
 *
 * <p>How security works here: - @AuthenticationPrincipal Jwt jwt --> Spring injects the decoded JWT
 * token that the frontend sent in the Authorization header. - We extract "realm_access.roles" from
 * that token to check if the caller has the "Government_Admin" role. - If not, we return 403
 * Forbidden.
 */
@RestController
@RequestMapping("/api/usermanagement")
@CrossOrigin(origins = "*")
public class UserManagementController {

  private static final Logger log = LoggerFactory.getLogger(UserManagementController.class);

  private final UserManagementService userManagementService;

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
   * Deletes a user by username.
   *
   * @param username username of the user to be deleted
   * @param jwt authenticated JWT of the caller
   * @return success or error response
   */
  @DeleteMapping("/delete")
  public ResponseEntity<?> deleteUser(
      @RequestParam String username, @AuthenticationPrincipal Jwt jwt) {

    if (!userManagementService.hasRole(jwt, "Government_Admin")) {
      return ResponseEntity.status(403)
          .body(Map.of("message", "Access denied. Only Government_Admin can delete users."));
    }

    try {
      userManagementService.deleteUser(username);
      return ResponseEntity.ok(
          Map.of("message", "User deleted successfully", "username", username));
    } catch (RuntimeException e) {
      log.error("Error deleting user: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  /**
   * Retrieves all users in the system.
   *
   * @param jwt authenticated JWT of the caller
   * @return list of users or an error response
   */
  @GetMapping("/users")
  public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal Jwt jwt) {

    if (!userManagementService.hasRole(jwt, "Government_Admin")) {
      return ResponseEntity.status(403)
          .body(Map.of("message", "Access denied. Only Government_Admin can view all users."));
    }

    List<UserRepresentation> users = userManagementService.getAllUsers();
    return ResponseEntity.ok(users);
  }
}
