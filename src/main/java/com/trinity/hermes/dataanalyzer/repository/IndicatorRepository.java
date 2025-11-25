package com.trinity.hermes.dataanalyzer.repository;



import com.trinity.hermes.dataanalyzer.model.Indicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, Long> {

    List<Indicator> findByIndicatorType(String indicatorType);

    List<Indicator> findByIndicatorTypeOrderByTimestampDesc(String indicatorType);

    @Query("SELECT i FROM Indicator i WHERE i.indicatorType = :type AND i.timestamp BETWEEN :startDate AND :endDate ORDER BY i.timestamp DESC")
    List<Indicator> findByIndicatorTypeAndDateRange(
            @Param("type") String indicatorType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT i FROM Indicator i WHERE i.indicatorType = :type ORDER BY i.timestamp DESC")
    List<Indicator> findLatestByIndicatorType(@Param("type") String indicatorType);
}