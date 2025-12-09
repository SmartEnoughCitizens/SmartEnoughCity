package com.trinity.hermes.Recommendation.service;

import com.trinity.hermes.Recommendation.dto.CreateRecommendationRequest;
import com.trinity.hermes.Recommendation.dto.RecommendationResponse;
import com.trinity.hermes.Recommendation.dto.UpdateRecommendationRequest;
import com.trinity.hermes.Recommendation.entity.Recommendation;
import com.trinity.hermes.Recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;

    // private void createDummyRecommendations() {
    //     Recommendation rec1 = new Recommendation();
    //     rec1.setId(idCounter.getAndIncrement());
    //     rec1.setName("Optimize Bus Route 42");
    //     rec1.setDescription("Reroute bus line 42 to avoid congested areas during peak hours");
    //     rec1.setStatus("PENDING");
    //     rec1.setCreatedAt(LocalDateTime.now().minusDays(2).toString());
    //     recommendations.put(rec1.getId(), rec1);

    //     Recommendation rec2 = new Recommendation();
    //     rec2.setId(idCounter.getAndIncrement());
    //     rec2.setName("Increase Train Frequency");
    //     rec2.setDescription("Add extra train services during morning rush hour on the Green Line");
    //     rec2.setStatus("APPROVED");
    //     rec2.setCreatedAt(LocalDateTime.now().minusDays(1).toString());
    //     recommendations.put(rec2.getId(), rec2);

    //     Recommendation rec3 = new Recommendation();
    //     rec3.setId(idCounter.getAndIncrement());
    //     rec3.setName("Traffic Signal Timing Adjustment");
    //     rec3.setDescription("Adjust traffic signal timing at Main St intersection to reduce congestion");
    //     rec3.setStatus("PENDING");
    //     rec3.setCreatedAt(LocalDateTime.now().toString());
    //     recommendations.put(rec3.getId(), rec3);

    //     log.info("Initialized {} dummy recommendations", recommendations.size());
    // }

    public List<RecommendationResponse> getAllRecommendations() {
        return recommendationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Optional<RecommendationResponse> getRecommendationById(Long id) {
        return recommendationRepository.findById(id)
                .map(this::mapToResponse);
    }

    public RecommendationResponse createRecommendation(CreateRecommendationRequest request) {
        Recommendation recommendation = new Recommendation();
        recommendation.setName(request.getName());
        recommendation.setDescription(request.getDescription());
        recommendation.setStatus("PENDING");
        Recommendation saved = recommendationRepository.save(recommendation);
        return mapToResponse(saved);
    }

    public Optional<RecommendationResponse> updateRecommendation(Long id, UpdateRecommendationRequest request) {
        return recommendationRepository.findById(id)
                .map(recommendation -> {
                    recommendation.setName(request.getName());
                    recommendation.setDescription(request.getDescription());
                    recommendation.setStatus(request.getStatus());
                    return mapToResponse(recommendationRepository.save(recommendation));
                });
    }

    public boolean deleteRecommendation(Long id) {
        if (recommendationRepository.existsById(id)) {
            recommendationRepository.deleteById(id);
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
                LocalDateTime.now()
        );
    }
}
