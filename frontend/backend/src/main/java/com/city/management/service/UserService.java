package com.city.management.service;

import com.city.management.model.Role;
import com.city.management.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(5);

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initUsers() {
        // Pre-seeded test users
        createUser(1L, "admin", "admin123", "admin@city.gov", "Admin User", Role.ROLE_GOVERNMENT_ADMIN);
        createUser(2L, "manager", "manager123", "manager@city.gov", "City Manager", Role.ROLE_CITY_MANAGER);
        createUser(3L, "provider_admin", "provider123", "provideradmin@city.gov", "Provider Admin", Role.ROLE_SERVICE_PROVIDER_ADMIN);
        createUser(4L, "provider_user", "user123", "provideruser@city.gov", "Provider User", Role.ROLE_SERVICE_PROVIDER_USER);
    }

    private void createUser(Long id, String username, String password, String email, String name, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        users.put(id, user);
        usersByUsername.put(username, user);
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    public User createUser(String username, String password, String email, String name, Role role) {
        Long id = idGenerator.getAndIncrement();
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        users.put(id, user);
        usersByUsername.put(username, user);
        return user;
    }

    public Optional<User> updateUser(Long id, User updatedUser) {
        User existingUser = users.get(id);
        if (existingUser == null) {
            return Optional.empty();
        }

        if (updatedUser.getEmail() != null) {
            existingUser.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getName() != null) {
            existingUser.setName(updatedUser.getName());
        }
        if (updatedUser.getRole() != null) {
            existingUser.setRole(updatedUser.getRole());
        }

        existingUser.setActive(updatedUser.isActive());

        return Optional.of(existingUser);
    }

    public boolean deleteUser(Long id) {
        User user = users.remove(id);
        if (user != null) {
            usersByUsername.remove(user.getUsername());
            return true;
        }
        return false;
    }

    public boolean validatePassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPassword());
    }

    public void updateLastLogin(String username) {
        User user = usersByUsername.get(username);
        if (user != null) {
            user.setLastLogin(LocalDateTime.now());
        }
    }
}
