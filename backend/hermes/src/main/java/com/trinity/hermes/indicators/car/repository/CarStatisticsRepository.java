package com.trinity.hermes.indicators.car.repository;

import com.trinity.hermes.indicators.car.entity.CarStatistics;
import com.trinity.hermes.indicators.car.entity.CarStatisticsId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CarStatisticsRepository extends JpaRepository<CarStatistics, CarStatisticsId> {

  @Query(
      value = "SELECT fuel_type, SUM(count) FROM external_data.vehicle_yearly GROUP BY fuel_type",
      nativeQuery = true)
  List<Object[]> findTotalCountByFuelType();

  @Query(
      value =
          "SELECT SUM(count) FROM external_data.vehicle_yearly "
              + "WHERE fuel_type = 'ELECTRIC' AND year >= 2015",
      nativeQuery = true)
  Long findElectricVehicleCountFrom2015();
}
