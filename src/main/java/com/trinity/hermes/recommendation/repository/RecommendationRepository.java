package com.trinity.hermes.Recommendation.repository;

import com.trinity.hermes.Recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
}
