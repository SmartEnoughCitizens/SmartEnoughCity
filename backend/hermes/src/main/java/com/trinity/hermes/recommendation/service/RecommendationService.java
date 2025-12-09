package com.trinity.hermes.recommendation.service;

import com.trinity.hermes.recommendation.dto.CreateRecommendationRequest;
import com.trinity.hermes.recommendation.dto.RecommendationResponse;
import com.trinity.hermes.recommendation.dto.UpdateRecommendationRequest;
import com.trinity.hermes.recommendation.entity.Recommendation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Recommendation Service - Using in-memory storage for thin slice.
 * Replace with JPA repository when database is properly set up.
 */
@Service
@Slf4j
public class RecommendationService {

    // In-memory storage for thin slice demo
    private final Map<Long, Recommendation> recommendations = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public RecommendationService() {
        // Initialize with some dummy recommendations for demo
        createDummyRecommendations();
    }

    private void createDummyRecommendations() {
        Recommendation rec1 = new Recommendation();
        rec1.setId(idCounter.getAndIncrement());
        rec1.setName("Optimize Bus Route 42");
        rec1.setDescription("Reroute bus line 42 to avoid congested areas during peak hours");
        rec1.setStatus("PENDING");
        rec1.setCreatedAt(LocalDateTime.now().minusDays(2).toString());
        recommendations.put(rec1.getId(), rec1);

        Recommendation rec2 = new Recommendation();
        rec2.setId(idCounter.getAndIncrement());
        rec2.setName("Increase Train Frequency");
        rec2.setDescription("Add extra train services during morning rush hour on the Green Line");
        rec2.setStatus("APPROVED");
        rec2.setCreatedAt(LocalDateTime.now().minusDays(1).toString());
        recommendations.put(rec2.getId(), rec2);

        Recommendation rec3 = new Recommendation();
        rec3.setId(idCounter.getAndIncrement());
        rec3.setName("Traffic Signal Timing Adjustment");
        rec3.setDescription("Adjust traffic signal timing at Main St intersection to reduce congestion");
        rec3.setStatus("PENDING");
        rec3.setCreatedAt(LocalDateTime.now().toString());
        recommendations.put(rec3.getId(), rec3);

        log.info("Initialized {} dummy recommendations", recommendations.size());
    }

    public List<RecommendationResponse> getAllRecommendations() {
        return recommendations.values()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Optional<RecommendationResponse> getRecommendationById(Long id) {
        return Optional.ofNullable(recommendations.get(id))
                .map(this::mapToResponse);
    }

    public RecommendationResponse createRecommendation(CreateRecommendationRequest request) {
        Recommendation recommendation = new Recommendation();
        recommendation.setId(idCounter.getAndIncrement());
        recommendation.setName(request.getName());
        recommendation.setDescription(request.getDescription());
        recommendation.setStatus("PENDING");
        recommendation.setCreatedAt(LocalDateTime.now().toString());
        recommendations.put(recommendation.getId(), recommendation);
        log.info("Created recommendation: {}", recommendation.getId());
        return mapToResponse(recommendation);
    }

    public Optional<RecommendationResponse> updateRecommendation(Long id, UpdateRecommendationRequest request) {
        Recommendation recommendation = recommendations.get(id);
        if (recommendation == null) {
            return Optional.empty();
        }
        recommendation.setName(request.getName());
        recommendation.setDescription(request.getDescription());
        recommendation.setStatus(request.getStatus());
        log.info("Updated recommendation: {}", id);
        return Optional.of(mapToResponse(recommendation));
    }

    public boolean deleteRecommendation(Long id) {
        if (recommendations.containsKey(id)) {
            recommendations.remove(id);
            log.info("Deleted recommendation: {}", id);
            return true;
        }
        return false;
    }

    private RecommendationResponse mapToResponse(Recommendation recommendation) {
        return new RecommendationResponse(
                recommendation.getId(),
                recommendation.getName(),
                recommendation.getDescription(),
                recommendation.getStatus(),
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
