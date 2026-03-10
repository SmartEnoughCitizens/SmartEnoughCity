package com.trinity.hermes.indicators.car.service;

import com.trinity.hermes.indicators.car.dto.CarDashboardDTO;
import com.trinity.hermes.indicators.car.repository.CarStatisticsRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarDashboardService {

  private final CarStatisticsRepository carStatisticsRepository;

  @Transactional(readOnly = true)
  public List<CarDashboardDTO> getFuelTypeStatistics() {
    log.info("Fetching car statistics grouped by fuel type");

    List<Object[]> rows = carStatisticsRepository.findTotalCountByFuelType();

    return rows.stream()
        .map(row -> CarDashboardDTO.builder()
            .fuelType((String) row[0])
            .count(((Number) row[1]).longValue())
            .build())
        .collect(Collectors.toList());
  }
}
