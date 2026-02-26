package com.trinity.hermes.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Returned to frontend after a user is created. Includes the Keycloak-generated user ID. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserResponse {

  private String userId; // Keycloak internal ID (UUID)
  private String username;
  private String email;
  private String role;
  private String message;
}
