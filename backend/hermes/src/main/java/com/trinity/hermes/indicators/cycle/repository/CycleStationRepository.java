package com.trinity.hermes.indicators.cycle.repository;

import com.trinity.hermes.indicators.cycle.entity.CycleStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CycleStationRepository extends JpaRepository<CycleStation, String> {
    List<CycleStation> findByNameContainingIgnoreCase(String name);

    Collection<CycleStation> findAvailableBikeStations();

    Collection<CycleStation> findAvailableDockStations();

    List<CycleStation> findByIsInstalledTrue();

    Double findAverageOccupancyRate();
}