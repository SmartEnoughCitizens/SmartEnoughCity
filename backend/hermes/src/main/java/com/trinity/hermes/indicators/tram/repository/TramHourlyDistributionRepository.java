package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.TramHourlyDistribution;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TramHourlyDistributionRepository
    extends JpaRepository<TramHourlyDistribution, Integer> {

  @Query(
      "SELECT h FROM TramHourlyDistribution h"
          + " WHERE h.year = :year"
          + " ORDER BY h.lineCode, h.timeCode")
  List<TramHourlyDistribution> findByYear(@Param("year") String year);

  @Query(
      "SELECT h FROM TramHourlyDistribution h"
          + " WHERE h.lineCode = :lineCode AND h.year = :year"
          + " ORDER BY h.timeCode")
  List<TramHourlyDistribution> findByLineCodeAndYear(
      @Param("lineCode") String lineCode, @Param("year") String year);

  @Query("SELECT MAX(h.year) FROM TramHourlyDistribution h")
  String findLatestYear();
}
