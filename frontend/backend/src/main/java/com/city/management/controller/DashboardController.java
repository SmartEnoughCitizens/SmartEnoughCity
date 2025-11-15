package com.city.management.controller;

import com.city.management.model.DashboardOverview;
import com.city.management.model.DashboardStats;
import com.city.management.service.MockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private MockDataService mockDataService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        return ResponseEntity.ok(mockDataService.getDashboardStats());
    }

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverview> getOverview() {
        return ResponseEntity.ok(mockDataService.getDashboardOverview());
    }
}
