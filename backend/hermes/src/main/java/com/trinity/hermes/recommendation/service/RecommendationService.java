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
