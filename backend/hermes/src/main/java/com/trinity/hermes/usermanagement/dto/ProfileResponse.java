package com.trinity.hermes.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

  private String username;
  private String email;
  private String firstName;
  private String lastName;
}
