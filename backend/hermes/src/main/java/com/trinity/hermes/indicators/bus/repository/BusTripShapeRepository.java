package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusTripShape;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusTripShapeRepository extends JpaRepository<BusTripShape, Integer> {

  @Query(
      "SELECT s FROM BusTripShape s WHERE s.shapeId = :shapeId ORDER BY s.ptSequence ASC")
  List<BusTripShape> findByShapeIdOrderByPtSequenceAsc(@Param("shapeId") String shapeId);
}
