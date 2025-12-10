package com.trinity.hermes.recommendation.repository;

import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
//TODO: Move it to a mongo repo
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findById(String id);

}
