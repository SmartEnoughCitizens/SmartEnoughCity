package com.trinity.hermes.indicators.events.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "events", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Events {

  @Id
  @Column(name = "id")
  private Integer id;

  @Column(name = "source")
  private String source;

  @Column(name = "source_id")
  private String sourceId;

  @Column(name = "event_name")
  private String eventName;

  @Column(name = "event_type")
  private String eventType;

  @Column(name = "venue_name")
  private String venueName;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "event_date")
  private LocalDate eventDate;

  @Column(name = "start_time")
  private LocalDateTime startTime;

  @Column(name = "end_time")
  private LocalDateTime endTime;

  @Column(name = "venue_id", insertable = false, updatable = false)
  private Integer venueId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venue_id", referencedColumnName = "id")
  private Venue venue;

  @Column(name = "estimated_attendance")
  private Integer estimatedAttendance;

  @Column(name = "fetched_at")
  private LocalDateTime fetchedAt;
}
