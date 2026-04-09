package com.trinity.hermes.recommendation.service;

import com.trinity.hermes.recommendation.dto.CreateRecommendationRequest;
import com.trinity.hermes.recommendation.dto.RecommendationResponse;
import com.trinity.hermes.recommendation.entity.Recommendation;
import com.trinity.hermes.recommendation.repository.RecommendationRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

  private final RecommendationRepository recommendationRepository;

  public List<RecommendationResponse> getAllRecommendations() {
    return recommendationRepository.findAll().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public Optional<RecommendationResponse> getRecommendationById(Integer id) {
    return recommendationRepository.findById(id).map(this::mapToResponse);
  }

  public RecommendationResponse createRecommendation(CreateRecommendationRequest request) {
    try {
      Recommendation recommendation =
          Recommendation.builder()
              .indicator(request.getIndicator())
              .recommendation(request.getRecommendation())
              .usecase(request.getUsecase())
              .simulation(request.getSimulation())
              .deleted(request.getDeleted())
              .status(request.getStatus())
              .build();
      Recommendation saved = recommendationRepository.save(recommendation);
      return mapToResponse(saved);
    } catch (Exception e) {
      log.error("Error while creating recommendation -", e);
      return null;
    }
  }

  public boolean deleteRecommendation(Integer id) {
    if (recommendationRepository.existsById(id)) {
      recommendationRepository.deleteById(id);
      return true;
    }
    return false;
  }

  public List<RecommendationResponse> getRecommendationsByIndicator(String indicator) {
    return recommendationRepository
        .findByIndicatorAndDeletedFalseOrderByCreatedAtDesc(indicator)
        .stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  private RecommendationResponse mapToResponse(Recommendation rec) {
    return RecommendationResponse.builder()
        .id(rec.getId())
        .indicator(rec.getIndicator())
        .recommendation(rec.getRecommendation())
        .usecase(rec.getUsecase())
        .simulation(rec.getSimulation())
        .createdAt(rec.getCreatedAt())
        .updatedAt(rec.getUpdatedAt())
        .deleted(rec.getDeleted())
        .status(rec.getStatus())
        .build();
  }
}
