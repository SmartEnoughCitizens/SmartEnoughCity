package com.city.management.service;

import com.city.management.model.JwtResponse;
import com.city.management.model.RegisterRequest;
import com.city.management.model.Role;
import com.city.management.model.User;
import com.city.management.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    public Optional<JwtResponse> login(String username, String password) {
        Optional<User> userOpt = userService.findByUsername(username);

        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!userService.validatePassword(user, password)) {
            return Optional.empty();
        }

        // Update last login
        userService.updateLastLogin(username);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        // Create response
        JwtResponse response = new JwtResponse();
        response.setToken(token);
        response.setUser(JwtResponse.UserDTO.fromUser(user));

        return Optional.of(response);
    }

    public Optional<JwtResponse> registerCityManager(RegisterRequest request) {
        return register(request, Role.ROLE_CITY_MANAGER);
    }

    public Optional<JwtResponse> registerServiceProviderAdmin(RegisterRequest request) {
        return register(request, Role.ROLE_SERVICE_PROVIDER_ADMIN);
    }

    private Optional<JwtResponse> register(RegisterRequest request, Role role) {
        // Check if username already exists
        if (userService.findByUsername(request.getUsername()).isPresent()) {
            return Optional.empty();
        }

        // Create new user
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getName(),
                role
        );

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        // Create response
        JwtResponse response = new JwtResponse();
        response.setToken(token);
        response.setUser(JwtResponse.UserDTO.fromUser(user));

        return Optional.of(response);
    }
}
