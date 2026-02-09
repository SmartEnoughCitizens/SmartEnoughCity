package com.trinity.hermes.usermanagement.controller;

import com.trinity.hermes.usermanagement.dto.RegisterUserRequest;
import com.trinity.hermes.usermanagement.dto.RegisterUserResponse;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
          // admins can be created only by City_Manager
          "Cycle_Admin", Set.of("City_Manager"),
          "Bus_Admin", Set.of("City_Manager"),
          "Train_Admin", Set.of("City_Manager"),
          "Tram_Admin", Set.of("City_Manager"),

          // providers can be created only by their respective admin
          "Cycle_Provider", Set.of("Cycle_Admin"),
          "Bus_Provider", Set.of("Bus_Admin"),
          "Train_Provider", Set.of("Train_Admin"),
          "Tram_Provider", Set.of("Tram_Admin"),
          "City_Manager", Set.of("Government_Admin"));

  public UserManagementController(UserManagementService userManagementService) {
    this.userManagementService = userManagementService;
  }

  // ---------------------------------------------------------------
  // 1. REGISTER USER
  //    Only Government_Admin can call this.
  // ---------------------------------------------------------------
  @PostMapping("/register")
  public ResponseEntity<?> registerUser(
      @Valid @RequestBody RegisterUserRequest request, @AuthenticationPrincipal Jwt jwt) {

    log.info("Received request to register user {}", request);

    // Check if the caller has Government_Admin role

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
      return ResponseEntity.created(null).body(response);
    } catch (RuntimeException e) {
      log.error("Error registering user: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  // ---------------------------------------------------------------
  // 2. DELETE USER
  //    Only Government_Admin can call this.
  // ---------------------------------------------------------------
  @DeleteMapping("/delete")
  public ResponseEntity<?> deleteUser(
      @RequestParam String username, @AuthenticationPrincipal Jwt jwt) {

    // Check if the caller has Government_Admin role
    if (!userManagementService.hasRole(jwt, "Government_Admin")) {
      return ResponseEntity.status(403)
          .body(Map.of("message", "Access denied. Only Government_Admin can delete users."));
    }

    // Prevent gov_admin from deleting itself
    String callerUsername = jwt.getClaimAsString("preferred_username");
    if (callerUsername != null && callerUsername.equalsIgnoreCase(username)) {
      return ResponseEntity.badRequest()
          .body(Map.of("message", "You cannot delete your own account."));
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

  // ---------------------------------------------------------------
  // 3. LIST ALL USERS (bonus)
  //    Only Government_Admin can call this.
  // ---------------------------------------------------------------
  @GetMapping("/users")
  public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal Jwt jwt) {

    if (!userManagementService.hasRole(jwt, "Government_Admin")) {
      return ResponseEntity.status(403)
          .body(Map.of("message", "Access denied. Only Government_Admin can view all users."));
    }

    List<UserRepresentation> users = userManagementService.getAllUsers();
    return ResponseEntity.ok(users);
  }

  @GetMapping("/sample")
  public ResponseEntity<?> getAllUsers() {

    log.info("Received request to get all sample ");
    return ResponseEntity.ok(null);
  }
}
