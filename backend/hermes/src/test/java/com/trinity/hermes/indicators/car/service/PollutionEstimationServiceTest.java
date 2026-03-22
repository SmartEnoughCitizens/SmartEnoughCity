package com.trinity.hermes.indicators.car.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.when;

import com.trinity.hermes.indicators.car.dto.FleetCompositionData;
import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.dto.JunctionEmissionDTO;
import com.trinity.hermes.indicators.car.repository.CarStatisticsRepository;
import com.trinity.hermes.indicators.car.repository.PrivateCarEmissionsRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PollutionEstimationServiceTest {

  @Mock private HighTrafficPointsService highTrafficPointsService;
  @Mock private PrivateCarEmissionsRepository privateCarEmissionsRepository;
  @Mock private CarStatisticsRepository carStatisticsRepository;

  @InjectMocks private PollutionEstimationService pollutionEstimationService;

  // --- getTrafficVolumeData ---

  @Test
  void getTrafficVolumeData_delegatesToHighTrafficPointsService() {
    HighTrafficPointsDTO point =
        HighTrafficPointsDTO.builder()
            .siteId(101)
            .lat(53.34)
            .lon(-6.26)
            .avgVolume(200.0)
            .dayType("weekday")
            .timeSlot("morning_peak")
            .build();
    when(highTrafficPointsService.getHighTrafficPoints()).thenReturn(List.of(point));

    List<HighTrafficPointsDTO> result = pollutionEstimationService.getTrafficVolumeData();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSiteId()).isEqualTo(101);
    assertThat(result.get(0).getAvgVolume()).isEqualTo(200.0);
  }

  // --- computeFleetComposition ---

  @Test
  void computeFleetComposition_withBandData_returnsCorrectRatios() {
    // privateCarTotal = 800 + 200 = 1000, evCount = 200, totalFleet = 1200
    when(privateCarEmissionsRepository.findEmissionBandCountsForDublin())
        .thenReturn(
            List.<Object[]>of(new Object[] {"BAND_A", 800L}, new Object[] {"BAND_B", 200L}));
    when(carStatisticsRepository.findElectricVehicleCountFrom2015()).thenReturn(200L);

    FleetCompositionData result = pollutionEstimationService.computeFleetComposition();

    assertThat(result.getTotalFleet()).isEqualTo(1200L);
    assertThat(result.getEvPercent()).isCloseTo(200.0 / 1200.0, offset(0.0001));
    assertThat(result.getIcePercent()).isCloseTo(1.0 - (200.0 / 1200.0), offset(0.0001));
    // BAND_A = 800/1000 = 0.8, BAND_B = 200/1000 = 0.2
    assertThat(result.getBandPercents().get("BAND_A")).isCloseTo(0.8, offset(0.0001));
    assertThat(result.getBandPercents().get("BAND_B")).isCloseTo(0.2, offset(0.0001));
  }

  @Test
  void computeFleetComposition_withNullEvCount_defaultsEvPercentToZero() {
    when(privateCarEmissionsRepository.findEmissionBandCountsForDublin())
        .thenReturn(List.<Object[]>of(new Object[] {"BAND_A", 1000L}));
    when(carStatisticsRepository.findElectricVehicleCountFrom2015()).thenReturn(null);

    FleetCompositionData result = pollutionEstimationService.computeFleetComposition();

    assertThat(result.getTotalFleet()).isEqualTo(1000L);
    assertThat(result.getEvPercent()).isEqualTo(0.0);
    assertThat(result.getIcePercent()).isEqualTo(1.0);
  }

  @Test
  void computeFleetComposition_withNoData_returnsZeroFleet() {
    when(privateCarEmissionsRepository.findEmissionBandCountsForDublin()).thenReturn(List.of());
    when(carStatisticsRepository.findElectricVehicleCountFrom2015()).thenReturn(0L);

    FleetCompositionData result = pollutionEstimationService.computeFleetComposition();

    assertThat(result.getTotalFleet()).isEqualTo(0L);
    assertThat(result.getEvPercent()).isEqualTo(0.0);
    assertThat(result.getBandPercents()).isEmpty();
  }

  // --- computeEmissions ---

  @Test
  void computeEmissions_vehicleVolumesAreSplitCorrectly() {
    // avgVolume = 100 → car=75, lcv=12, bus=5, hgv=5, motorcycle=3
    when(highTrafficPointsService.getHighTrafficPoints())
        .thenReturn(List.of(junction(101, 100.0, "weekday", "morning_peak")));
    mockFleet("BAND_A", 1000L, 0L);

    List<JunctionEmissionDTO> result = pollutionEstimationService.computeEmissions();

    assertThat(result).hasSize(1);
    JunctionEmissionDTO dto = result.get(0);
    assertThat(dto.getCarVolume()).isEqualTo(75.0);
    assertThat(dto.getLcvVolume()).isEqualTo(12.0);
    assertThat(dto.getBusVolume()).isEqualTo(5.0);
    assertThat(dto.getHgvVolume()).isEqualTo(5.0);
    assertThat(dto.getMotorcycleVolume()).isEqualTo(3.0);
  }

  @Test
  void computeEmissions_totalEmissionIsGreaterThanZero() {
    when(highTrafficPointsService.getHighTrafficPoints())
        .thenReturn(List.of(junction(101, 100.0, "weekday", "morning_peak")));
    mockFleet("BAND_A", 1000L, 0L);

    List<JunctionEmissionDTO> result = pollutionEstimationService.computeEmissions();

    assertThat(result.get(0).getTotalEmissionG()).isGreaterThan(0.0);
  }

  @Test
  void computeEmissions_junctionMetadataIsPreserved() {
    when(highTrafficPointsService.getHighTrafficPoints())
        .thenReturn(List.of(junction(202, 50.0, "weekend", "off_peak")));
    mockFleet("BAND_C", 500L, 100L);

    List<JunctionEmissionDTO> result = pollutionEstimationService.computeEmissions();

    JunctionEmissionDTO dto = result.get(0);
    assertThat(dto.getSiteId()).isEqualTo(202);
    assertThat(dto.getLat()).isEqualTo(53.35);
    assertThat(dto.getLon()).isEqualTo(-6.27);
    assertThat(dto.getDayType()).isEqualTo("weekend");
    assertThat(dto.getTimeSlot()).isEqualTo("off_peak");
  }

  @Test
  void computeEmissions_multipleJunctions_returnsOneResultPerJunction() {
    when(highTrafficPointsService.getHighTrafficPoints())
        .thenReturn(
            List.of(
                junction(101, 100.0, "weekday", "morning_peak"),
                junction(102, 200.0, "weekday", "evening_peak"),
                junction(103, 50.0, "weekend", "off_peak")));
    mockFleet("BAND_A", 1000L, 0L);

    List<JunctionEmissionDTO> result = pollutionEstimationService.computeEmissions();

    assertThat(result).hasSize(3);
  }

  @Test
  void computeEmissions_higherVolumeProducesHigherEmission() {
    when(highTrafficPointsService.getHighTrafficPoints())
        .thenReturn(
            List.of(
                junction(101, 100.0, "weekday", "morning_peak"),
                junction(102, 200.0, "weekday", "morning_peak")));
    mockFleet("BAND_A", 1000L, 0L);

    List<JunctionEmissionDTO> result = pollutionEstimationService.computeEmissions();

    JunctionEmissionDTO low =
        result.stream().filter(d -> d.getSiteId() == 101).findFirst().orElseThrow();
    JunctionEmissionDTO high =
        result.stream().filter(d -> d.getSiteId() == 102).findFirst().orElseThrow();
    assertThat(high.getTotalEmissionG()).isGreaterThan(low.getTotalEmissionG());
  }

  // --- Helpers ---

  private HighTrafficPointsDTO junction(
      int siteId, double avgVolume, String dayType, String timeSlot) {
    return HighTrafficPointsDTO.builder()
        .siteId(siteId)
        .lat(53.35)
        .lon(-6.27)
        .avgVolume(avgVolume)
        .dayType(dayType)
        .timeSlot(timeSlot)
        .build();
  }

  private void mockFleet(String band, long bandCount, long evCount) {
    when(privateCarEmissionsRepository.findEmissionBandCountsForDublin())
        .thenReturn(List.<Object[]>of(new Object[] {band, bandCount}));
    when(carStatisticsRepository.findElectricVehicleCountFrom2015()).thenReturn(evCount);
  }
}
