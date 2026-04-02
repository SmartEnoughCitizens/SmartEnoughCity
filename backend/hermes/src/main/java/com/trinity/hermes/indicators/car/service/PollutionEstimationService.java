package com.trinity.hermes.indicators.car.service;

import com.trinity.hermes.indicators.car.dto.FleetCompositionData;
import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.dto.JunctionEmissionDTO;
import com.trinity.hermes.indicators.car.repository.CarStatisticsRepository;
import com.trinity.hermes.indicators.car.repository.PrivateCarEmissionsRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollutionEstimationService {

  private final HighTrafficPointsService highTrafficPointsService;
  private final PrivateCarEmissionsRepository privateCarEmissionsRepository;
  private final CarStatisticsRepository carStatisticsRepository;

  // Emission factors in grams of CO2 per kilometre, keyed by emission band / vehicle type.
  // Bands A–G correspond to Irish motor tax CO2 classification bands.
  // Midpoints are used for bounded ranges; fixed values are used for LCV, Bus, HGV and EV.
  static final Map<String, Double> EMISSION_FACTORS_G_PER_KM =
      Map.ofEntries(
          Map.entry("BAND_A", 100.0), // under 120 g/km
          Map.entry("BAND_B", 130.0), // 121–140 g/km
          Map.entry("BAND_C", 148.0), // 141–155 g/km
          Map.entry("BAND_D", 163.0), // 156–170 g/km
          Map.entry("BAND_E", 181.0), // 171–190 g/km
          Map.entry("BAND_F", 208.0), // 191–225 g/km
          Map.entry("BAND_G", 250.0), // over 225 g/km
          Map.entry("LCV", 180.0), // Light Commercial Vehicle
          Map.entry("BUS", 900.0), // Bus
          Map.entry("HGV", 700.0), // Heavy Goods Vehicle
          Map.entry("MOTORCYCLE", 100.0), // Motorcycle
          Map.entry("EV", 0.0) // Electric Vehicle
          );

  /**
   * Returns average vehicle volumes per junction, broken down by day type (weekday/weekend) and
   * time slot (morning_peak, inter_peak, evening_peak, off_peak).
   */
  public List<HighTrafficPointsDTO> getTrafficVolumeData() {
    log.info("Extracting traffic volume data per junction from HighTrafficPointsService");
    return highTrafficPointsService.getHighTrafficPoints();
  }

  /**
   * Computes fleet composition ratios from the aggregated SQL data:
   *
   * <ul>
   *   <li>total_fleet = sum(private_car_emissions counts) + EV count from vehicle_yearly
   *   <li>ev_percent = EV count / total_fleet
   *   <li>ice_percent = 1 - ev_percent
   *   <li>band_X_percent = count of band X / sum(private_car_emissions counts)
   * </ul>
   */
  @Transactional(readOnly = true)
  public FleetCompositionData computeFleetComposition() {
    log.info("Computing fleet composition from aggregated emission and vehicle data");

    // Retrieve per-band counts for Dublin, year >= 2015: [(emission_band, sum_count), ...]
    List<Object[]> bandRows = privateCarEmissionsRepository.findEmissionBandCountsForDublin();

    // Total private car count (sum across all bands)
    long privateCarTotal = bandRows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

    // Electric vehicle count from vehicle_yearly, year >= 2015
    Long evCount = carStatisticsRepository.findElectricVehicleCountFrom2015();
    long evCountSafe = evCount != null ? evCount : 0L;

    long totalFleet = privateCarTotal + evCountSafe;

    double evPercent = totalFleet > 0 ? (double) evCountSafe / totalFleet : 0.0;
    double icePercent = 1.0 - evPercent;

    // Per-band share of the private-car fleet
    Map<String, Double> bandPercents =
        bandRows.stream()
            .collect(
                Collectors.toMap(
                    r -> (String) r[0],
                    r ->
                        privateCarTotal > 0
                            ? ((Number) r[1]).longValue() / (double) privateCarTotal
                            : 0.0));

    log.info(
        "Fleet composition — totalFleet={}, evPercent={}, icePercent={}, bands={}",
        totalFleet,
        evPercent,
        icePercent,
        bandPercents.keySet());

    return FleetCompositionData.builder()
        .totalFleet(totalFleet)
        .evPercent(evPercent)
        .icePercent(icePercent)
        .bandPercents(bandPercents)
        .build();
  }

  /**
   * For each junction row from HighTrafficPointsService, computes vehicle type volumes and total
   * CO2 emissions (moving + idle) across all vehicle types and emission bands.
   *
   * <p>Vehicle type split from avg_volume:
   *
   * <ul>
   *   <li>Car 75%, LCV 12%, Bus 5%, HGV 5%, Motorcycle 3%
   * </ul>
   *
   * Within car volume: EV 15%, ICE bands 85% (distributed by dynamic band percentages).
   */
  @Cacheable("junctionEmissions")
  @Transactional(readOnly = true)
  public List<JunctionEmissionDTO> computeEmissions() {
    log.info("Computing junction emissions");

    List<HighTrafficPointsDTO> trafficRows = getTrafficVolumeData();
    FleetCompositionData fleet = computeFleetComposition();
    Map<String, Double> bandPercents = fleet.getBandPercents();

    return trafficRows.stream()
        .map(row -> buildJunctionEmission(row, bandPercents))
        .collect(Collectors.toList());
  }

  private JunctionEmissionDTO buildJunctionEmission(
      HighTrafficPointsDTO row, Map<String, Double> bandPercents) {

    double avgVolume = row.getAvgVolume();

    // --- Vehicle type volumes ---
    double carVolume = 0.75 * avgVolume;
    double lcvVolume = 0.12 * avgVolume;
    double busVolume = 0.05 * avgVolume;
    double hgvVolume = 0.05 * avgVolume;
    double motorcycleVolume = 0.03 * avgVolume;

    // --- Within car volume: 15% EV, 85% ICE distributed by band percentages ---
    double evVolume = 0.15 * carVolume;
    double iceBandTotal = 0.85 * carVolume;

    // --- Compute total emission across all vehicle types ---
    double totalEmission = 0.0;

    // Non-car vehicle types
    totalEmission += calculateEmission(lcvVolume, EMISSION_FACTORS_G_PER_KM.get("LCV"));
    totalEmission += calculateEmission(busVolume, EMISSION_FACTORS_G_PER_KM.get("BUS"));
    totalEmission += calculateEmission(hgvVolume, EMISSION_FACTORS_G_PER_KM.get("HGV"));
    totalEmission +=
        calculateEmission(motorcycleVolume, EMISSION_FACTORS_G_PER_KM.get("MOTORCYCLE"));

    // EV (emission factor = 0, included for completeness)
    totalEmission += calculateEmission(evVolume, EMISSION_FACTORS_G_PER_KM.get("EV"));

    // ICE bands A–G using dynamic band percentages
    for (Map.Entry<String, Double> entry : bandPercents.entrySet()) {
      String band = entry.getKey();
      double bandVolume = entry.getValue() * iceBandTotal;
      Double emissionFactor = EMISSION_FACTORS_G_PER_KM.get(band);
      if (emissionFactor != null) {
        totalEmission += calculateEmission(bandVolume, emissionFactor);
      }
    }

    return JunctionEmissionDTO.builder()
        .siteId(row.getSiteId())
        .lat(row.getLat())
        .lon(row.getLon())
        .dayType(row.getDayType())
        .timeSlot(row.getTimeSlot())
        .carVolume(carVolume)
        .lcvVolume(lcvVolume)
        .busVolume(busVolume)
        .hgvVolume(hgvVolume)
        .motorcycleVolume(motorcycleVolume)
        .totalEmissionG(totalEmission)
        .build();
  }

  /**
   * Calculates combined moving and idle CO2 emission for a single vehicle type.
   *
   * <p>Moving: vehicleCount × 0.6 × 0.5 km × emissionFactor (g/km) <br>
   * Idle: vehicleCount × 0.4 × 1.5 min × idleRate (g/min) <br>
   * where idleRate = (emissionFactor × 20 km/h) ÷ 60
   */
  private double calculateEmission(double vehicleCount, double emissionFactorGPerKm) {
    double movingEmission = vehicleCount * 0.6 * 0.5 * emissionFactorGPerKm;
    double idleRate = (emissionFactorGPerKm * 20.0) / 60.0;
    double idleEmission = vehicleCount * 0.4 * 1.5 * idleRate;
    return movingEmission + idleEmission;
  }
}
