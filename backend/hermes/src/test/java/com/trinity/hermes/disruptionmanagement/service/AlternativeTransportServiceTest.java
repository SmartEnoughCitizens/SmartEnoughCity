package com.trinity.hermes.disruptionmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trinity.hermes.disruptionmanagement.dto.AlternativeTransportResult;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.entity.DisruptionAlternative;
import com.trinity.hermes.disruptionmanagement.repository.AlternativeTransportRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlternativeTransportServiceTest {

  @Mock private AlternativeTransportRepository alternativeTransportRepository;

  @InjectMocks private AlternativeTransportService service;

  private Disruption disruption;

  @BeforeEach
  void setUp() {
    disruption = new Disruption();
    disruption.setId(1L);
    disruption.setLatitude(53.3498);
    disruption.setLongitude(-6.2603);
  }

  // ── getAlternatives ────────────────────────────────────────────────

  @Test
  void getAlternatives_returnsAlternativesForDisruptionWithCoords() {
    AlternativeTransportResult busResult =
        new AlternativeTransportResult(
            "bus", "stop_1", "College Green", 53.34, -6.26, null, null, 120);
    AlternativeTransportResult bikeResult =
        new AlternativeTransportResult(
            "bike", "db_12", "Earlsfort Terrace", 53.33, -6.25, 5, 20, 300);

    when(alternativeTransportRepository.findNearby(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of(busResult, bikeResult));

    List<DisruptionAlternative> alts = service.getAlternatives(disruption);

    assertThat(alts).hasSize(2);
    assertThat(alts)
        .extracting(DisruptionAlternative::getMode)
        .containsExactlyInAnyOrder("bus", "bike");
  }

  @Test
  void getAlternatives_nullLatitude_returnsEmpty() {
    disruption.setLatitude(null);

    List<DisruptionAlternative> alts = service.getAlternatives(disruption);

    assertThat(alts).isEmpty();
    verify(alternativeTransportRepository, never()).findNearby(anyDouble(), anyDouble(), anyInt());
  }

  @Test
  void getAlternatives_nullLongitude_returnsEmpty() {
    disruption.setLongitude(null);

    List<DisruptionAlternative> alts = service.getAlternatives(disruption);

    assertThat(alts).isEmpty();
    verify(alternativeTransportRepository, never()).findNearby(anyDouble(), anyDouble(), anyInt());
  }

  @Test
  void getAlternatives_noNearbyOptions_returnsEmpty() {
    when(alternativeTransportRepository.findNearby(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of());

    List<DisruptionAlternative> alts = service.getAlternatives(disruption);

    assertThat(alts).isEmpty();
  }

  // ── description building ───────────────────────────────────────────

  @Test
  void getAlternatives_busResult_descriptionContainsBusStop() {
    AlternativeTransportResult busResult =
        new AlternativeTransportResult(
            "bus", "stop_1", "Westmoreland St", 53.34, -6.26, null, null, 80);
    when(alternativeTransportRepository.findNearby(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of(busResult));

    List<DisruptionAlternative> alts = service.getAlternatives(disruption);

    assertThat(alts.get(0).getDescription()).contains("Bus stop").contains("Westmoreland St");
  }

  @Test
  void getAlternatives_bikeResult_descriptionContainsAvailableBikes() {
    AlternativeTransportResult bikeResult =
        new AlternativeTransportResult("bike", "db_5", "Merrion Square", 53.33, -6.25, 8, 30, 200);
    when(alternativeTransportRepository.findNearby(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of(bikeResult));

    List<DisruptionAlternative> alts = service.getAlternatives(disruption);

    assertThat(alts.get(0).getDescription()).contains("8 bikes available");
  }

  @Test
  void getAlternatives_railResult_descriptionContainsIrishRail() {
    AlternativeTransportResult railResult =
        new AlternativeTransportResult("rail", "CNLY", "Connolly", 53.35, -6.25, null, null, 450);
    when(alternativeTransportRepository.findNearby(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of(railResult));

    List<DisruptionAlternative> alts = service.getAlternatives(disruption);

    assertThat(alts.get(0).getDescription()).contains("Irish Rail").contains("Connolly");
  }

  @Test
  void getAlternatives_disruptionLinkedOnAllAlternatives() {
    AlternativeTransportResult r =
        new AlternativeTransportResult("bus", "stop_1", "Stop A", 53.34, -6.26, null, null, 100);
    when(alternativeTransportRepository.findNearby(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of(r));

    List<DisruptionAlternative> alts = service.getAlternatives(disruption);

    assertThat(alts).allSatisfy(a -> assertThat(a.getDisruption()).isSameAs(disruption));
  }

  // ── findNearby convenience overload ───────────────────────────────

  @Test
  void findNearby_delegatesToRepository() {
    AlternativeTransportResult r =
        new AlternativeTransportResult("rail", "PEARSE", "Pearse", 53.34, -6.25, null, null, 250);
    when(alternativeTransportRepository.findNearby(53.3498, -6.2603, 500)).thenReturn(List.of(r));

    List<AlternativeTransportResult> results = service.findNearby(53.3498, -6.2603);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).stopName()).isEqualTo("Pearse");
  }
}
