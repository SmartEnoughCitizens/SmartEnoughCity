package com.trinity.hermes.recommendation.facade;

import com.trinity.hermes.recommendation.dto.CreateRecommendationRequest;
import com.trinity.hermes.recommendation.dto.RecommendationResponse;
import com.trinity.hermes.recommendation.dto.UpdateRecommendationRequest;
import com.trinity.hermes.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RecommendationFacade {

    private final RecommendationService recommendationService;

    public List<RecommendationResponse> getAllRecommendations() {
        return recommendationService.getAllRecommendations();
    }

    public Optional<RecommendationResponse> getRecommendationById(Long id) {
        return recommendationService.getRecommendationById(id);
    }

    public RecommendationResponse createRecommendation(CreateRecommendationRequest request) {
        return recommendationService.createRecommendation(request);
    }

//    public Optional<RecommendationResponse> updateRecommendation(Long id, UpdateRecommendationRequest request) {
//        return recommendationService.updateRecommendation(id, request);
//    }

    public boolean deleteRecommendation(Long id) {
        return recommendationService.deleteRecommendation(id);
    }
}
