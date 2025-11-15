package com.city.management.controller;

import com.city.management.model.SystemStatus;
import com.city.management.service.MockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private MockDataService mockDataService;

    @GetMapping("/status")
    public ResponseEntity<SystemStatus> getSystemStatus() {
        return ResponseEntity.ok(mockDataService.getSystemStatus());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        return ResponseEntity.ok(mockDataService.getSystemHealth());
    }
}
