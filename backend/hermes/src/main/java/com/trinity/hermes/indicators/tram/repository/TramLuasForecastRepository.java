package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.TramLuasForecast;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TramLuasForecastRepository extends JpaRepository<TramLuasForecast, Integer> {

  List<TramLuasForecast> findByLine(String line);

  List<TramLuasForecast> findByStopId(String stopId);

  @Query("SELECT COUNT(f) FROM TramLuasForecast f")
  long countAllForecasts();

  @Query("SELECT COUNT(f) FROM TramLuasForecast f WHERE f.line = :line")
  long countByLine(@Param("line") String line);

  @Query("SELECT COALESCE(AVG(f.dueMins), 0.0) FROM TramLuasForecast f WHERE f.dueMins IS NOT NULL")
  Double findAverageDueMins();

  @Query("SELECT f FROM TramLuasForecast f ORDER BY f.line, f.stopId, f.direction")
  List<TramLuasForecast> findAllOrderedByLineAndStop();
}
