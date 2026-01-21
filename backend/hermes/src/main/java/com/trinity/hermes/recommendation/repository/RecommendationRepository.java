package com.trinity.hermes.recommendation.repository;

import com.trinity.hermes.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// TODO: Move it to a mongo repo
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {}
