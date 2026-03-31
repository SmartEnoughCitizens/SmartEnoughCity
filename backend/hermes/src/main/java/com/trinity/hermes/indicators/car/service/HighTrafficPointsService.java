package com.trinity.hermes.indicators.car.service;

import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.repository.HighTrafficPointsRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HighTrafficPointsService {

  private final HighTrafficPointsRepository highTrafficPointsRepository;

  @Cacheable("highTrafficPoints")
  @Transactional(readOnly = true)
  public List<HighTrafficPointsDTO> getHighTrafficPoints() {
    log.info("Fetching aggregated traffic points with location data");

    List<Object[]> rows = highTrafficPointsRepository.findAggregatedTrafficWithLocation();

    // SQL already returns one row per (siteId, dayType, timeSlot) — map directly to DTOs.
    return rows.stream()
        .map(
            row -> {
              Integer siteId = ((Number) row[0]).intValue();
              Double lat = row[1] != null ? ((Number) row[1]).doubleValue() : null;
              Double lon = row[2] != null ? ((Number) row[2]).doubleValue() : null;
              String dayType = (String) row[3];
              String timeSlot = (String) row[4];
              Double avgVolume = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
              return HighTrafficPointsDTO.builder()
                  .siteId(siteId)
                  .lat(lat)
                  .lon(lon)
                  .dayType(dayType)
                  .timeSlot(timeSlot)
                  .avgVolume(avgVolume)
                  .build();
            })
        .collect(Collectors.toList());
  }
}
