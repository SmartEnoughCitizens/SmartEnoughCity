package com.trinity.hermes.indicators.train.repository;

import com.trinity.hermes.indicators.train.entity.TrainStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainStationRepository extends JpaRepository<TrainStation, Long> {
    TrainStation findByStationCode(String stationCode);
}
