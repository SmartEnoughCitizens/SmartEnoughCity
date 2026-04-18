package com.trinity.hermes.recommendation.repository;

import com.trinity.hermes.recommendation.entity.Recommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

@Repository
// TODO: Move it to a mongo repo
public interface RecommendationRepository extends JpaRepository<Recommendation, Integer> {

  @Query(
      "SELECT r FROM Recommendation r WHERE LOWER(r.indicator) = LOWER(:indicator) AND (r.deleted IS NULL OR r.deleted = false) AND r.status = 'pending'")
  List<Recommendation> findActiveByIndicator(@Param("indicator") String indicator);

  @Modifying
  @Transactional
  @Query(
      "UPDATE Recommendation r SET r.status = 'submitted' WHERE LOWER(r.indicator) = LOWER(:indicator) AND r.status = 'pending'")
  void markSubmittedByIndicator(@Param("indicator") String indicator);
}
