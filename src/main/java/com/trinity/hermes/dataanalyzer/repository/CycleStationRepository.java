package com.trinity.hermes.dataanalyzer.repository;

import com.trinity.hermes.dataanalyzer.model.CycleStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CycleStationRepository extends JpaRepository<CycleStation, Long> {

    Optional<CycleStation> findByStationId(String stationId);

    List<CycleStation> findByIsRentingTrue();

    List<CycleStation> findByIsInstalledTrue();

    @Query("SELECT c FROM CycleStation c WHERE c.numBikesAvailable > 0 AND c.isRenting = true")
    List<CycleStation> findAvailableBikeStations();

    @Query("SELECT c FROM CycleStation c WHERE c.numDocksAvailable > 0 AND c.isReturning = true")
    List<CycleStation> findAvailableDockStations();

    @Query("SELECT AVG(c.numBikesAvailable) FROM CycleStation c WHERE c.isInstalled = true")
    Double findAverageBikesAvailable();

    @Query("SELECT AVG(CAST(c.numBikesAvailable AS double) / NULLIF(c.capacity, 0) * 100) FROM CycleStation c WHERE c.capacity > 0")
    Double findAverageOccupancyRate();

    @Query("SELECT c FROM CycleStation c WHERE c.lat BETWEEN :minLat AND :maxLat AND c.lon BETWEEN :minLon AND :maxLon")
    List<CycleStation> findStationsInBounds(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );
}