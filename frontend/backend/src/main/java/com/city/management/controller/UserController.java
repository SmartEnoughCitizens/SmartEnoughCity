package com.city.management.controller;

import com.city.management.model.JwtResponse;
import com.city.management.model.Role;
import com.city.management.model.User;
import com.city.management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<JwtResponse.UserDTO>> getAllUsers() {
        List<JwtResponse.UserDTO> users = userService.findAll().stream()
                .map(JwtResponse.UserDTO::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/service-provider")
    public ResponseEntity<?> createServiceProviderUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getName(),
                Role.ROLE_SERVICE_PROVIDER_USER
        );

        return ResponseEntity.ok(JwtResponse.UserDTO.fromUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        User updatedUser = new User();
        updatedUser.setEmail(request.getEmail());
        updatedUser.setName(request.getName());
        if (request.getRole() != null) {
            updatedUser.setRole(Role.valueOf(request.getRole()));
        }
        updatedUser.setActive(request.isActive());

        Optional<User> result = userService.updateUser(id, updatedUser);

        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(JwtResponse.UserDTO.fromUser(result.get()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // Request DTOs
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String email;
        private String name;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class UpdateUserRequest {
        private String email;
        private String name;
        private String role;
        private boolean active;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
