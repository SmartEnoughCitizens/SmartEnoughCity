package com.trinity.hermes.indicators.car.repository;

import com.trinity.hermes.indicators.car.entity.PrivateCarEmissions;
import com.trinity.hermes.indicators.car.entity.PrivateCarEmissionsId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivateCarEmissionsRepository
    extends JpaRepository<PrivateCarEmissions, PrivateCarEmissionsId> {

  @Query(
      value =
          "SELECT emission_band, SUM(count) FROM external_data.private_car_emissions "
              + "WHERE year >= 2015 AND licensing_authority = 'Dublin' "
              + "AND emission_band != 'NA' "
              + "GROUP BY emission_band",
      nativeQuery = true)
  List<Object[]> findEmissionBandCountsForDublin();
}
