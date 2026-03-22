package com.trinity.hermes.indicators.car.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.trinity.hermes.indicators.car.dto.CarDashboardDTO;
import com.trinity.hermes.indicators.car.repository.CarStatisticsRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CarDashboardServiceTest {

  @Mock private CarStatisticsRepository carStatisticsRepository;
  @InjectMocks private CarDashboardService carDashboardService;

  @Test
  void getFuelTypeStatistics_returnsMappedDtos() {
    when(carStatisticsRepository.findTotalCountByFuelType())
        .thenReturn(
            List.<Object[]>of(new Object[] {"PETROL", 5000L}, new Object[] {"ELECTRIC", 1200L}));

    List<CarDashboardDTO> result = carDashboardService.getFuelTypeStatistics();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getFuelType()).isEqualTo("PETROL");
    assertThat(result.get(0).getCount()).isEqualTo(5000L);
    assertThat(result.get(1).getFuelType()).isEqualTo("ELECTRIC");
    assertThat(result.get(1).getCount()).isEqualTo(1200L);
  }

  @Test
  void getFuelTypeStatistics_withEmptyData_returnsEmptyList() {
    when(carStatisticsRepository.findTotalCountByFuelType()).thenReturn(List.of());

    List<CarDashboardDTO> result = carDashboardService.getFuelTypeStatistics();

    assertThat(result).isEmpty();
  }

  @Test
  void getFuelTypeStatistics_withMultipleFuelTypes_returnsAllMapped() {
    when(carStatisticsRepository.findTotalCountByFuelType())
        .thenReturn(
            List.<Object[]>of(
                new Object[] {"PETROL", 5000L},
                new Object[] {"DIESEL", 3000L},
                new Object[] {"ELECTRIC", 1200L},
                new Object[] {"HYBRID", 800L}));

    List<CarDashboardDTO> result = carDashboardService.getFuelTypeStatistics();

    assertThat(result).hasSize(4);
    assertThat(result)
        .extracting(CarDashboardDTO::getFuelType)
        .containsExactly("PETROL", "DIESEL", "ELECTRIC", "HYBRID");
  }

  @Test
  void getFuelTypeStatistics_withIntegerCount_convertsToLong() {
    when(carStatisticsRepository.findTotalCountByFuelType())
        .thenReturn(List.<Object[]>of(new Object[] {"DIESEL", 3000}));

    List<CarDashboardDTO> result = carDashboardService.getFuelTypeStatistics();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCount()).isEqualTo(3000L);
  }
}
