package com.trinity.hermes.indicators.events.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventsDTO {

  private Integer id;
  private String eventName;
  private String eventType;
  private String venueName;
  private Double latitude;
  private Double longitude;
  private LocalDate eventDate;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Integer estimatedAttendance;
}
