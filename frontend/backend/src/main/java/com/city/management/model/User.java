// package com.city.management.model;

// import lombok.AllArgsConstructor;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// import java.time.LocalDateTime;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
// public class User {
//     private Long id;
//     private String username;
//     private String password;
//     private String email;
//     private String name;
//     private Role role;
//     private boolean active;
//     private LocalDateTime createdAt;
//     private LocalDateTime lastLogin;

//     // Constructor for basic user creation
//     public User(Long id, String username, String password, Role role) {
//         this.id = id;
//         this.username = username;
//         this.password = password;
//         this.role = role;
//         this.active = true;
//         this.createdAt = LocalDateTime.now();
//         this.email = username + "@city.gov";
//         this.name = username;
//     }
// }

package com.city.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String name;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}