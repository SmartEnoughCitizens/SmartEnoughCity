package com.trinity.hermes.Recommendation.controller;

import com.trinity.hermes.Recommendation.dto.CreateRecommendationRequest;
import com.trinity.hermes.Recommendation.dto.RecommendationResponse;
import com.trinity.hermes.Recommendation.dto.UpdateRecommendationRequest;
import com.trinity.hermes.Recommendation.facade.RecommendationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationFacade recommendationFacade;

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getAllRecommendations() {
        return ResponseEntity.ok(recommendationFacade.getAllRecommendations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecommendationResponse> getRecommendationById(@PathVariable Long id) {
        Optional<RecommendationResponse> recommendation = recommendationFacade.getRecommendationById(id);
        return recommendation.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RecommendationResponse> createRecommendation(@RequestBody CreateRecommendationRequest request) {
        RecommendationResponse created = recommendationFacade.createRecommendation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecommendationResponse> updateRecommendation(@PathVariable Long id, @RequestBody UpdateRecommendationRequest request) {
        Optional<RecommendationResponse> updated = recommendationFacade.updateRecommendation(id, request);
        return updated.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecommendation(@PathVariable Long id) {
        boolean deleted = recommendationFacade.deleteRecommendation(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}

