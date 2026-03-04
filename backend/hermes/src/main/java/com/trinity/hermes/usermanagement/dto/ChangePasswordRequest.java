package com.trinity.hermes.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {

  @NotBlank(message = "New password is required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  private String newPassword;
}
