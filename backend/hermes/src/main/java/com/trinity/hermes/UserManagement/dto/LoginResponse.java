package com.trinity.hermes.UserManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private int expiresIn;
    private String refreshToken;
    private String username;
    private String message;
}
