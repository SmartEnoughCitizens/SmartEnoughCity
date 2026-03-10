package com.trinity.hermes.indicators.car.service;

import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.repository.HighTrafficPointsRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HighTrafficPointsService {

  private final HighTrafficPointsRepository highTrafficPointsRepository;

  private record ClassifiedRow(
      Integer siteId, Double lat, Double lon, Double volume, String dayType, String timeSlot) {}

  @Transactional(readOnly = true)
  public List<HighTrafficPointsDTO> getHighTrafficPoints() {
    log.info("Fetching aggregated traffic points with location data");

    List<Object[]> rows = highTrafficPointsRepository.findAggregatedTrafficWithLocation();

    // Step 1: classify each raw row with dayType and timeSlot
    List<ClassifiedRow> classified = rows.stream().map(this::classify).collect(Collectors.toList());

    // Step 2: group by (siteId, dayType, timeSlot) and compute average volume
    Map<String, List<ClassifiedRow>> grouped =
        classified.stream()
            .collect(
                Collectors.groupingBy(r -> r.siteId() + "|" + r.dayType() + "|" + r.timeSlot()));

    return grouped.values().stream()
        .map(
            group -> {
              ClassifiedRow first = group.get(0);
              double avgVolume =
                  group.stream().mapToDouble(ClassifiedRow::volume).average().orElse(0.0);
              return HighTrafficPointsDTO.builder()
                  .siteId(first.siteId())
                  .lat(first.lat())
                  .lon(first.lon())
                  .avgVolume(avgVolume)
                  .dayType(first.dayType())
                  .timeSlot(first.timeSlot())
                  .build();
            })
        .collect(Collectors.toList());
  }

  private ClassifiedRow classify(Object[] row) {
    Integer siteId = ((Number) row[0]).intValue();
    LocalDateTime dateTime = ((java.sql.Timestamp) row[1]).toLocalDateTime();
    Double volume = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
    Double lat = row[3] != null ? ((Number) row[3]).doubleValue() : null;
    Double lon = row[4] != null ? ((Number) row[4]).doubleValue() : null;

    LocalDate date = dateTime.toLocalDate();
    LocalTime time = dateTime.toLocalTime();

    String dayType = isWeekend(date) ? "weekend" : "weekday";
    String timeSlot = determineTimeSlot(time);

    return new ClassifiedRow(siteId, lat, lon, volume, dayType, timeSlot);
  }

  private boolean isWeekend(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
  }

  private String determineTimeSlot(LocalTime time) {
    LocalTime morningStart = LocalTime.of(7, 0);
    LocalTime interPeakStart = LocalTime.of(10, 0);
    LocalTime eveningPeakStart = LocalTime.of(16, 0);
    LocalTime offPeakStart = LocalTime.of(19, 0);

    if (!time.isBefore(morningStart) && time.isBefore(interPeakStart)) {
      return "morning_peak";
    } else if (!time.isBefore(interPeakStart) && time.isBefore(eveningPeakStart)) {
      return "inter_peak";
    } else if (!time.isBefore(eveningPeakStart) && time.isBefore(offPeakStart)) {
      return "evening_peak";
    } else {
      return "off_peak";
    }
  }
}
