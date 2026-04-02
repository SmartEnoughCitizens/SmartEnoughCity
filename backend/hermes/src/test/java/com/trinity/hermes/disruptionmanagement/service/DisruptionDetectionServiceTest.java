package com.trinity.hermes.disruptionmanagement.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.facade.DisruptionFacade;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionRepository;
import com.trinity.hermes.indicators.bus.repository.BusRouteMetricsRepository;
import com.trinity.hermes.indicators.car.repository.HighTrafficPointsRepository;
import com.trinity.hermes.indicators.events.repository.EventsRepository;
import com.trinity.hermes.indicators.train.entity.TrainStation;
import com.trinity.hermes.indicators.train.entity.TrainStationData;
import com.trinity.hermes.indicators.train.repository.TrainStationDataRepository;
import com.trinity.hermes.indicators.train.repository.TrainStationRepository;
import com.trinity.hermes.indicators.tram.entity.TramLuasForecast;
import com.trinity.hermes.indicators.tram.entity.TramStop;
import com.trinity.hermes.indicators.tram.repository.TramLuasForecastRepository;
import com.trinity.hermes.indicators.tram.repository.TramStopRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisruptionDetectionServiceTest {

  @Mock private BusRouteMetricsRepository busRouteMetricsRepository;
  @Mock private HighTrafficPointsRepository highTrafficPointsRepository;
  @Mock private EventsRepository eventsRepository;
  @Mock private DisruptionRepository disruptionRepository;
  @Mock private DisruptionFacade disruptionFacade;
  @Mock private TrainStationDataRepository trainStationDataRepository;
  @Mock private TrainStationRepository trainStationRepository;
  @Mock private TramLuasForecastRepository tramLuasForecastRepository;
  @Mock private TramStopRepository tramStopRepository;

  @InjectMocks private DisruptionDetectionService service;

  @BeforeEach
  void stubCommonRepos() {
    // Prevent NPE in bus/car/event/train/tram detection for tests that only care about one mode
    when(busRouteMetricsRepository.findCandidatesForDisruptionDetection()).thenReturn(List.of());
    when(highTrafficPointsRepository.findAggregatedTrafficWithLocation()).thenReturn(List.of());
    when(eventsRepository.findUpcomingEvents(any())).thenReturn(List.of());
    when(trainStationDataRepository.findLatestPerStationTrain()).thenReturn(List.of());
    when(tramLuasForecastRepository.findAll()).thenReturn(List.of());
    when(tramStopRepository.findAll()).thenReturn(List.of());
    // Default: no recent duplicates
    when(disruptionRepository.findByDisruptionTypeAndAffectedAreaAndDetectedAtAfter(
            anyString(), anyString(), any()))
        .thenReturn(List.of());
  }

  // ── Train detection ────────────────────────────────────────────────

  @Test
  void detectDisruptions_threeLateTrams_createsTrainDisruption() {
    TrainStationData late1 = lateTrainAt("CNLY", "E801", 15);
    TrainStationData late2 = lateTrainAt("CNLY", "E802", 22); // >20 → MEDIUM severity (not skipped)
    TrainStationData late3 = lateTrainAt("CNLY", "E803", 12);
    when(trainStationDataRepository.findLatestPerStationTrain())
        .thenReturn(List.of(late1, late2, late3));

    TrainStation station = new TrainStation(1, "CNLY", "Connolly", null, 53.3504, -6.2496, null);
    when(trainStationRepository.findByStationCode("CNLY")).thenReturn(station);

    service.detectDisruptions();

    verify(disruptionFacade).handleDisruptionDetection(
        argThat(r -> "DELAY".equals(r.getDisruptionType())
            && "TRAIN".equals(r.getAffectedTransportModes().get(0))));
  }

  @Test
  void detectDisruptions_fewerThanThreeLateTrams_noTrainDisruption() {
    TrainStationData late1 = lateTrainAt("CNLY", "E801", 15);
    TrainStationData late2 = lateTrainAt("CNLY", "E802", 18);
    // Only 2 late — below threshold of 3
    when(trainStationDataRepository.findLatestPerStationTrain())
        .thenReturn(List.of(late1, late2));

    service.detectDisruptions();

    verify(disruptionFacade, never()).handleDisruptionDetection(any());
  }

  @Test
  void detectDisruptions_lateMinutesBelowThreshold_noTrainDisruption() {
    // lateMinutes = 5 (below the 10-min threshold)
    TrainStationData late1 = lateTrainAt("CNLY", "E801", 5);
    TrainStationData late2 = lateTrainAt("CNLY", "E802", 5);
    TrainStationData late3 = lateTrainAt("CNLY", "E803", 5);
    when(trainStationDataRepository.findLatestPerStationTrain())
        .thenReturn(List.of(late1, late2, late3));

    service.detectDisruptions();

    verify(disruptionFacade, never()).handleDisruptionDetection(any());
  }

  @Test
  void detectDisruptions_multipleStations_onlyDisruptedStationTriggered() {
    // CNLY: 3 late trains → disruption
    TrainStationData cnly1 = lateTrainAt("CNLY", "E801", 15);
    TrainStationData cnly2 = lateTrainAt("CNLY", "E802", 21); // >20 → MEDIUM severity (not skipped)
    TrainStationData cnly3 = lateTrainAt("CNLY", "E803", 12);
    // PEAR: only 1 late train → no disruption
    TrainStationData pear1 = lateTrainAt("PEAR", "E901", 11);

    when(trainStationDataRepository.findLatestPerStationTrain())
        .thenReturn(List.of(cnly1, cnly2, cnly3, pear1));
    TrainStation cnlyStation = new TrainStation(1, "CNLY", "Connolly", null, 53.35, -6.25, null);
    when(trainStationRepository.findByStationCode("CNLY")).thenReturn(cnlyStation);

    service.detectDisruptions();

    // Only one disruption (for CNLY) — PEAR doesn't meet the ≥3 threshold
    verify(disruptionFacade).handleDisruptionDetection(any());
  }

  // ── Tram detection ─────────────────────────────────────────────────

  @Test
  void detectDisruptions_highDueMins_createsTramDisruption() {
    // Off-peak: expected freq = 10 min. due_mins = 25 → delay = 15 ≥ threshold (10) → MEDIUM
    TramLuasForecast forecast = tramForecastAt("STS", "green", 25);
    when(tramLuasForecastRepository.findAll()).thenReturn(List.of(forecast));

    TramStop stop = tramStopAt("STS", "green", "St. Stephen's Green", 53.3382, -6.2591);
    when(tramStopRepository.findAll()).thenReturn(List.of(stop));

    service.detectDisruptions();

    verify(disruptionFacade).handleDisruptionDetection(
        argThat(r -> "TRAM_DISRUPTION".equals(r.getDisruptionType())
            && "TRAM".equals(r.getAffectedTransportModes().get(0))));
  }

  @Test
  void detectDisruptions_lowDueMins_noTramDisruption() {
    // Off-peak: expected freq = 10 min. due_mins = 12 → delay = 2 < threshold (10) → no disruption
    TramLuasForecast forecast = tramForecastAt("STS", "green", 12);
    when(tramLuasForecastRepository.findAll()).thenReturn(List.of(forecast));

    TramStop stop = tramStopAt("STS", "green", "St. Stephen's Green", 53.3382, -6.2591);
    when(tramStopRepository.findAll()).thenReturn(List.of(stop));

    service.detectDisruptions();

    verify(disruptionFacade, never()).handleDisruptionDetection(any());
  }

  @Test
  void detectDisruptions_noTramForecasts_noTramDisruption() {
    // Empty forecasts — data not loaded yet, no false positives
    when(tramLuasForecastRepository.findAll()).thenReturn(List.of());

    service.detectDisruptions();

    verify(disruptionFacade, never()).handleDisruptionDetection(any());
  }

  @Test
  void detectDisruptions_deduplication_skipsAlreadyDetectedDisruption() {
    TrainStationData late1 = lateTrainAt("CNLY", "E801", 15);
    TrainStationData late2 = lateTrainAt("CNLY", "E802", 22); // >20 → MEDIUM, so dedup check is reached
    TrainStationData late3 = lateTrainAt("CNLY", "E803", 12);
    when(trainStationDataRepository.findLatestPerStationTrain())
        .thenReturn(List.of(late1, late2, late3));

    // Simulate: a disruption for this station was already detected within the dedup window
    Disruption existing = new Disruption();
    existing.setId(99L);
    when(disruptionRepository.findByDisruptionTypeAndAffectedAreaAndDetectedAtAfter(
            anyString(), anyString(), any()))
        .thenReturn(List.of(existing));
    when(trainStationRepository.findByStationCode("CNLY"))
        .thenReturn(new TrainStation(1, "CNLY", "Connolly", null, 53.35, -6.25, null));

    service.detectDisruptions();

    verify(disruptionFacade, never()).handleDisruptionDetection(any());
  }

  // ── Helpers ────────────────────────────────────────────────────────

  private TrainStationData lateTrainAt(String stationCode, String trainCode, int lateMinutes) {
    TrainStationData d = new TrainStationData();
    d.setStationCode(stationCode);
    d.setTrainCode(trainCode);
    d.setLateMinutes(lateMinutes);
    return d;
  }

  private TramLuasForecast tramForecastAt(String stopId, String line, int dueMins) {
    TramLuasForecast f = new TramLuasForecast();
    f.setStopId(stopId);
    f.setLine(line);
    f.setDirection("Outbound");
    f.setDestination("Bride's Glen");
    f.setDueMins(dueMins);
    f.setMessage("");
    return f;
  }

  private TramStop tramStopAt(String stopId, String line, String name, double lat, double lon) {
    TramStop s = new TramStop();
    s.setStopId(stopId);
    s.setLine(line);
    s.setName(name);
    s.setLat(lat);
    s.setLon(lon);
    return s;
  }

  /** Mockito argThat shorthand — avoids static import clash with assertThat. */
  private static <T> T argThat(org.mockito.ArgumentMatcher<T> matcher) {
    return org.mockito.ArgumentMatchers.argThat(matcher);
  }
}
