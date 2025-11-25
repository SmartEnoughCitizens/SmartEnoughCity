package com.trinity.hermes.UserManagement.Controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api")
@Slf4j
public class UserManagementController {

    // Public endpoint - no authentication needed
    @GetMapping("/public/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Hermes Service is running!");
        return ResponseEntity.ok(response);
    }

    // Trains - Only city_manager can access
    @GetMapping("/trains")
    public ResponseEntity<Map<String, Object>> resource1() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("User {} accessing Trains", auth.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("resource", "Trains");
        response.put("message", "This is accessible ONLY by city_manager");
        response.put("user", auth.getName());
        response.put("roles", auth.getAuthorities().toString());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    // Bus - Both city_manager and bus_provider can access
    @GetMapping("/buses")
    public ResponseEntity<Map<String, Object>> resource2() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("User {} accessing buses", auth.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("resource", "buses");
        response.put("message", "This is accessible by BOTH city_manager and bus_provider");
        response.put("user", auth.getName());
        response.put("roles", auth.getAuthorities().toString());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}



//package com.trinity.hermes.UserManagement.Controller;
//
//import com.trinity.hermes.UserManagement.Entity.User;
//import com.trinity.hermes.UserManagement.Service.UserManagementService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//public class UserManagementController {
//
//    @Autowired
//    UserManagementService userManagementService;
//
//   @GetMapping("/greet")
//    public String greet(){
//       return "Welcome to Smart City !";
//   }
//
//   @PostMapping("/login")
//    public ResponseEntity<String> login(@RequestBody User user){
//       return userManagementService.login(user);
//   }
//
//}
