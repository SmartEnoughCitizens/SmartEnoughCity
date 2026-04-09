package com.trinity.hermes.recommendation.repository;

import com.trinity.hermes.recommendation.entity.Recommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
// TODO: Move it to a mongo repo
public interface RecommendationRepository extends JpaRepository<Recommendation, Integer> {

  @Query(
      "SELECT r FROM Recommendation r WHERE LOWER(r.indicator) = LOWER(:indicator) AND (r.deleted IS NULL OR r.deleted = false)")
  List<Recommendation> findActiveByIndicator(@Param("indicator") String indicator);
}