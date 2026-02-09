package com.trinity.hermes.usermanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Frontend sends this JSON to POST /api/usermanagement/register
 *
 * <p>Example request body: { "username": "bus_provider_1", "email": "provider1@buslimited.com",
 * "firstName": "John", "lastName": "Doe", "role": "Bus_Provider", "password": "TempPass@123" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {

  @NotBlank(message = "Username is required")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Must be a valid email")
  private String email;

  @NotBlank(message = "First name is required")
  private String firstName;

  @NotBlank(message = "Last name is required")
  private String lastName;

  // Only these roles are allowed -- gov_admin cannot assign Government_Admin to others
  @NotBlank(message = "Role is required")
  @Pattern(
      regexp =
          "City_Manager|Bus_Admin|Bus_Provider|Cycle_Admin|Cycle_Provider|Train_Admin|Train_Provider|Tram_Admin|Tram_Provider",
      message =
          "Invalid role. Allowed: City_Manager, Bus_Admin, Bus_Provider, Cycle_Admin, Cycle_Provider, Train_Admin, Train_Provider, Tram_Admin, Tram_Provider")
  private String role;

  // Optional: if not provided, a temporary password is set and user must change on first login
  private String password;
}
