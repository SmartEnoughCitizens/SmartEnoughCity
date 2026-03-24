package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.TramTripShape;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TramTripShapeRepository extends JpaRepository<TramTripShape, Integer> {

  @Query("SELECT s FROM TramTripShape s WHERE s.shapeId = :shapeId ORDER BY s.ptSequence ASC")
  List<TramTripShape> findByShapeIdOrderByPtSequenceAsc(@Param("shapeId") String shapeId);
}
