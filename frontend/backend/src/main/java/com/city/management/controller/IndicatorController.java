package com.city.management.controller;

import com.city.management.model.IndicatorData;
import com.city.management.service.MockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/indicators")
public class IndicatorController {

    @Autowired
    private MockDataService mockDataService;

    @GetMapping("/{mode}")
    public ResponseEntity<IndicatorData> getIndicatorData(@PathVariable String mode) {
        // Valid modes: car, cycle, bus, train, tram, pedestrian, events, construction
        return ResponseEntity.ok(mockDataService.getIndicatorData(mode));
    }
}
