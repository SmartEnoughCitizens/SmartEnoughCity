package com.trinity.hermes.indicators.car.facade;

import com.trinity.hermes.indicators.car.dto.CarDashboardDTO;
import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.service.CarDashboardService;
import com.trinity.hermes.indicators.car.service.HighTrafficPointsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarFacade {

  private final CarDashboardService carDashboardService;
  private final HighTrafficPointsService highTrafficPointsService;

  public List<CarDashboardDTO> getFuelTypeStatistics() {
    return carDashboardService.getFuelTypeStatistics();
  }

  public List<HighTrafficPointsDTO> getHighTrafficPoints() {
    return highTrafficPointsService.getHighTrafficPoints();
  }
}
