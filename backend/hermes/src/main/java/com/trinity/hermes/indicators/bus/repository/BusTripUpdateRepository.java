package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusTripUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusTripUpdateRepository extends JpaRepository<BusTripUpdate, Long> {

    List<BusTripUpdate> findByRouteId(String routeId);

    List<BusTripUpdate> findByRouteIdOrderByIdDesc(String routeId);

    @Query("SELECT b FROM BusTripUpdate b WHERE b.routeId = :routeId ORDER BY b.id DESC")
    List<BusTripUpdate> findLatestByRouteId(@Param("routeId") String routeId);

    List<BusTripUpdate> findByStopId(String stopId);

    @Query("SELECT DISTINCT b.routeId FROM BusTripUpdate b")
    List<String> findAllDistinctRouteIds();

    @Query("SELECT AVG(b.arrivalDelay) FROM BusTripUpdate b WHERE b.routeId = :routeId AND b.arrivalDelay IS NOT NULL")
    Double findAverageArrivalDelayByRouteId(@Param("routeId") String routeId);

    @Query("SELECT AVG(b.departureDelay) FROM BusTripUpdate b WHERE b.routeId = :routeId AND b.departureDelay IS NOT NULL")
    Double findAverageDepartureDelayByRouteId(@Param("routeId") String routeId);
}