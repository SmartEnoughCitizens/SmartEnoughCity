package com.trinity.hermes.disruptionmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.entity.DisruptionCause;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionRepository;
import com.trinity.hermes.indicators.car.repository.HighTrafficPointsRepository;
import com.trinity.hermes.indicators.events.entity.Events;
import com.trinity.hermes.indicators.events.entity.Venue;
import com.trinity.hermes.indicators.events.repository.EventsRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CauseCorrelationServiceTest {

  @Mock private EventsRepository eventsRepository;
  @Mock private HighTrafficPointsRepository highTrafficPointsRepository;
  @Mock private DisruptionRepository disruptionRepository;

  @InjectMocks private CauseCorrelationService service;

  private Disruption disruption;

  @BeforeEach
  void setUp() {
    disruption = new Disruption();
    disruption.setId(1L);
    disruption.setDisruptionType("DELAY");
    disruption.setAffectedTransportModes(List.of("BUS"));
    disruption.setDetectedAt(LocalDateTime.now(ZoneId.of("Europe/Dublin")));
  }

  // ── EVENT cause ────────────────────────────────────────────────────

  @Test
  void correlateCauses_largeEventToday_addsEventCauseWithHighConfidence() {
    Events event = largeEventAt("Aviva Stadium", 15000);
    when(eventsRepository.findUpcomingEventsAtLargeVenues(anyInt(), any()))
        .thenReturn(List.of(event));
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation()).thenReturn(List.of());
    when(disruptionRepository.findAllActiveOrderByDetectedAtDesc()).thenReturn(List.of());

    List<DisruptionCause> causes = service.correlateCauses(disruption);

    assertThat(causes)
        .anySatisfy(
            c -> {
              assertThat(c.getCauseType()).isEqualTo("EVENT");
              assertThat(c.getConfidence()).isEqualTo("HIGH");
              assertThat(c.getCauseDescription()).contains("Aviva Stadium");
            });
  }

  @Test
  void correlateCauses_noEvents_noEventCause() {
    when(eventsRepository.findUpcomingEventsAtLargeVenues(anyInt(), any())).thenReturn(List.of());
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation()).thenReturn(List.of());
    when(disruptionRepository.findAllActiveOrderByDetectedAtDesc()).thenReturn(List.of());

    List<DisruptionCause> causes = service.correlateCauses(disruption);

    assertThat(causes).noneMatch(c -> "EVENT".equals(c.getCauseType()));
  }

  // ── CONGESTION cause ───────────────────────────────────────────────

  @Test
  void correlateCauses_highTraffic_addsCongestionCauseWithMediumConfidence() {
    Object[] trafficRow = {"site_1", "region_A", 2000L, 53.3, -6.2};
    when(eventsRepository.findUpcomingEventsAtLargeVenues(anyInt(), any())).thenReturn(List.of());
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(List.<Object[]>of(trafficRow));
    when(disruptionRepository.findAllActiveOrderByDetectedAtDesc()).thenReturn(List.of());

    List<DisruptionCause> causes = service.correlateCauses(disruption);

    assertThat(causes)
        .anySatisfy(
            c -> {
              assertThat(c.getCauseType()).isEqualTo("CONGESTION");
              assertThat(c.getConfidence()).isEqualTo("MEDIUM");
            });
  }

  @Test
  void correlateCauses_lowTraffic_noCongestionCause() {
    Object[] trafficRow = {"site_1", "region_A", 500L, 53.3, -6.2}; // below threshold
    when(eventsRepository.findUpcomingEventsAtLargeVenues(anyInt(), any())).thenReturn(List.of());
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(List.<Object[]>of(trafficRow));
    when(disruptionRepository.findAllActiveOrderByDetectedAtDesc()).thenReturn(List.of());

    List<DisruptionCause> causes = service.correlateCauses(disruption);

    assertThat(causes).noneMatch(c -> "CONGESTION".equals(c.getCauseType()));
  }

  // ── CROSS_MODE cause ───────────────────────────────────────────────

  @Test
  void correlateCauses_anotherModeDisrupted_addsCrossModeCauseWithLowConfidence() {
    Disruption tramDisruption = new Disruption();
    tramDisruption.setId(2L);
    tramDisruption.setAffectedTransportModes(List.of("TRAM")); // different mode
    tramDisruption.setDetectedAt(
        LocalDateTime.now(ZoneId.of("Europe/Dublin")).minusMinutes(5)); // within window

    when(eventsRepository.findUpcomingEventsAtLargeVenues(anyInt(), any())).thenReturn(List.of());
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation()).thenReturn(List.of());
    when(disruptionRepository.findAllActiveOrderByDetectedAtDesc())
        .thenReturn(List.of(tramDisruption));

    List<DisruptionCause> causes = service.correlateCauses(disruption);

    assertThat(causes)
        .anySatisfy(
            c -> {
              assertThat(c.getCauseType()).isEqualTo("CROSS_MODE");
              assertThat(c.getConfidence()).isEqualTo("LOW");
            });
  }

  @Test
  void correlateCauses_sameModeDisrupted_noCrossModeCause() {
    Disruption otherBus = new Disruption();
    otherBus.setId(2L);
    otherBus.setAffectedTransportModes(List.of("BUS")); // same mode — not cross-mode
    otherBus.setDetectedAt(LocalDateTime.now(ZoneId.of("Europe/Dublin")).minusMinutes(5));

    when(eventsRepository.findUpcomingEventsAtLargeVenues(anyInt(), any())).thenReturn(List.of());
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation()).thenReturn(List.of());
    when(disruptionRepository.findAllActiveOrderByDetectedAtDesc()).thenReturn(List.of(otherBus));

    List<DisruptionCause> causes = service.correlateCauses(disruption);

    assertThat(causes).noneMatch(c -> "CROSS_MODE".equals(c.getCauseType()));
  }

  // ── Multiple causes ────────────────────────────────────────────────

  @Test
  void correlateCauses_noCauses_returnsEmptyList() {
    when(eventsRepository.findUpcomingEventsAtLargeVenues(anyInt(), any())).thenReturn(List.of());
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation()).thenReturn(List.of());
    when(disruptionRepository.findAllActiveOrderByDetectedAtDesc()).thenReturn(List.of());

    List<DisruptionCause> causes = service.correlateCauses(disruption);

    assertThat(causes).isEmpty();
  }

  @Test
  void correlateCauses_allCausesPresent_returnsThreeCauses() {
    Events event = largeEventAt("3Arena", 13000);

    Object[] trafficRow = {"site_1", "region_A", 2000L, 53.3, -6.2};

    Disruption tramDisruption = new Disruption();
    tramDisruption.setId(99L);
    tramDisruption.setAffectedTransportModes(List.of("TRAM"));
    tramDisruption.setDetectedAt(LocalDateTime.now(ZoneId.of("Europe/Dublin")).minusMinutes(3));

    when(eventsRepository.findUpcomingEventsAtLargeVenues(anyInt(), any()))
        .thenReturn(List.of(event));
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation())
        .thenReturn(List.<Object[]>of(trafficRow));
    when(disruptionRepository.findAllActiveOrderByDetectedAtDesc())
        .thenReturn(List.of(tramDisruption));

    List<DisruptionCause> causes = service.correlateCauses(disruption);

    assertThat(causes).hasSize(3);
    assertThat(causes)
        .extracting(DisruptionCause::getCauseType)
        .containsExactlyInAnyOrder("EVENT", "CONGESTION", "CROSS_MODE");
  }

  @Test
  void correlateCauses_disruptionLinkedOnAllCauses() {
    Events event = largeEventAt("RDS", 6000);
    when(eventsRepository.findUpcomingEventsAtLargeVenues(anyInt(), any()))
        .thenReturn(List.of(event));
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation()).thenReturn(List.of());
    when(disruptionRepository.findAllActiveOrderByDetectedAtDesc()).thenReturn(List.of());

    List<DisruptionCause> causes = service.correlateCauses(disruption);

    assertThat(causes).allSatisfy(c -> assertThat(c.getDisruption()).isSameAs(disruption));
  }

  // ── Helpers ────────────────────────────────────────────────────────

  private Events largeEventAt(String venueName, int capacity) {
    Venue venue = new Venue(1, null, venueName, null, "Dublin", null, null, capacity);
    Events event = new Events();
    event.setEventName("Test Event");
    event.setVenueName(venueName);
    event.setVenue(venue);
    return event;
  }
}
