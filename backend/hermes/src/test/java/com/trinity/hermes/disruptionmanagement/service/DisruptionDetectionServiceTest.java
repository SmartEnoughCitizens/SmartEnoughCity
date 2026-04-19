package com.trinity.hermes.disruptionmanagement.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trinity.hermes.disruptionmanagement.facade.DisruptionFacade;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionRepository;
import com.trinity.hermes.indicators.bus.repository.BusLiveStopTimeUpdateRepository;
import com.trinity.hermes.indicators.bus.repository.BusStopRepository;
import com.trinity.hermes.indicators.car.repository.HighTrafficPointsRepository;
import com.trinity.hermes.indicators.train.entity.TrainStation;
import com.trinity.hermes.indicators.train.entity.TrainStationData;
import com.trinity.hermes.indicators.train.repository.TrainStationDataRepository;
import com.trinity.hermes.indicators.train.repository.TrainStationRepository;
import com.trinity.hermes.indicators.tram.entity.TramLuasForecast;
import com.trinity.hermes.indicators.tram.entity.TramStop;
import com.trinity.hermes.indicators.tram.repository.TramLuasForecastRepository;
import com.trinity.hermes.indicators.tram.repository.TramStopRepository;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
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

  @Mock private BusLiveStopTimeUpdateRepository busLiveStopTimeUpdateRepository;
  @Mock private BusStopRepository busStopRepository;
  @Mock private HighTrafficPointsRepository highTrafficPointsRepository;
  @Mock private DisruptionRepository disruptionRepository;
  @Mock private DisruptionFacade disruptionFacade;
  @Mock private TrainStationDataRepository trainStationDataRepository;
  @Mock private TrainStationRepository trainStationRepository;
  @Mock private TramLuasForecastRepository tramLuasForecastRepository;
  @Mock private TramStopRepository tramStopRepository;

  @InjectMocks private DisruptionDetectionService service;

  @BeforeEach
  void stubCommonRepos() {
    when(busLiveStopTimeUpdateRepository.findWorstDelayedStopPerRoute(
            anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of());
    when(highTrafficPointsRepository.findPeakTrafficSitesFromMv()).thenReturn(List.of());
    when(highTrafficPointsRepository.findPeakTrafficSitesWithLocation()).thenReturn(List.of());
    when(trainStationDataRepository.findLatestPerStationTrainFromMv()).thenReturn(List.of());
    when(trainStationDataRepository.findLatestPerStationTrain(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of());
    when(tramLuasForecastRepository.findAll()).thenReturn(List.of());
    when(tramStopRepository.findAll()).thenReturn(List.of());
    when(busStopRepository.findRouteShortNamesNear(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of());
    when(tramStopRepository.findStopsNear(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of());
    when(disruptionRepository.findExpiredActiveDisruptions(any())).thenReturn(List.of());
  }

  // ── Train detection ────────────────────────────────────────────────

  @Test
  void detectDisruptions_threeLateTrams_createsTrainDisruption() {
    TrainStationData late1 = lateTrainAt("CNLY", "E801", 15);
    TrainStationData late2 = lateTrainAt("CNLY", "E802", 22);
    TrainStationData late3 = lateTrainAt("CNLY", "E803", 12);
    when(trainStationDataRepository.findLatestPerStationTrainFromMv())
        .thenThrow(new RuntimeException("MV not available"));
    when(trainStationDataRepository.findLatestPerStationTrain(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of(late1, late2, late3));

    TrainStation station = new TrainStation(1, "CNLY", "Connolly", null, 53.3504, -6.2496, null);
    when(trainStationRepository.findByStationCode("CNLY")).thenReturn(station);

    service.detectDisruptions();

    verify(disruptionFacade)
        .handleDisruptionDetection(
            argThat(
                r ->
                    "DELAY".equals(r.getDisruptionType())
                        && "TRAIN".equals(r.getAffectedTransportModes().get(0))));
  }

  @Test
  void detectDisruptions_fewerThanThreeLateTrams_noTrainDisruption() {
    TrainStationData late1 = lateTrainAt("CNLY", "E801", 15);
    TrainStationData late2 = lateTrainAt("CNLY", "E802", 18);
    when(trainStationDataRepository.findLatestPerStationTrain(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of(late1, late2));

    service.detectDisruptions();

    verify(disruptionFacade, never()).handleDisruptionDetection(any());
  }

  @Test
  void detectDisruptions_lateMinutesBelowThreshold_noTrainDisruption() {
    TrainStationData late1 = lateTrainAt("CNLY", "E801", 5);
    TrainStationData late2 = lateTrainAt("CNLY", "E802", 5);
    TrainStationData late3 = lateTrainAt("CNLY", "E803", 5);
    when(trainStationDataRepository.findLatestPerStationTrain(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of(late1, late2, late3));

    service.detectDisruptions();

    verify(disruptionFacade, never()).handleDisruptionDetection(any());
  }

  @Test
  void detectDisruptions_multipleStations_onlyDisruptedStationTriggered() {
    TrainStationData cnly1 = lateTrainAt("CNLY", "E801", 15);
    TrainStationData cnly2 = lateTrainAt("CNLY", "E802", 21);
    TrainStationData cnly3 = lateTrainAt("CNLY", "E803", 12);
    TrainStationData pear1 = lateTrainAt("PEAR", "E901", 11);

    when(trainStationDataRepository.findLatestPerStationTrainFromMv())
        .thenThrow(new RuntimeException("MV not available"));
    when(trainStationDataRepository.findLatestPerStationTrain(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of(cnly1, cnly2, cnly3, pear1));
    TrainStation cnlyStation = new TrainStation(1, "CNLY", "Connolly", null, 53.35, -6.25, null);
    when(trainStationRepository.findByStationCode("CNLY")).thenReturn(cnlyStation);

    service.detectDisruptions();

    verify(disruptionFacade).handleDisruptionDetection(any());
  }

  // ── Tram detection ─────────────────────────────────────────────────

  @Test
  void detectDisruptions_highDueMins_createsTramDisruption() {
    // Tram detection skips 01:00–05:59 Dublin time (Luas not running). Skip test in that window.
    int hour = LocalTime.now(ZoneId.of("Europe/Dublin")).getHour();
    Assumptions.assumeTrue(hour < 1 || hour >= 6, "Skipping: tram early-return window (01–06)");

    // Off-peak: expected freq = 10 min. due_mins = 25 → delay = 15 ≥ threshold (10) → MEDIUM
    TramLuasForecast forecast = tramForecastAt("STS", "green", 25);
    when(tramLuasForecastRepository.findAll()).thenReturn(List.of(forecast));

    TramStop stop = tramStopAt("STS", "green", "St. Stephen's Green", 53.3382, -6.2591);
    when(tramStopRepository.findAll()).thenReturn(List.of(stop));

    service.detectDisruptions();

    verify(disruptionFacade)
        .handleDisruptionDetection(
            argThat(
                r ->
                    "TRAM_DISRUPTION".equals(r.getDisruptionType())
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
    when(tramLuasForecastRepository.findAll()).thenReturn(List.of());

    service.detectDisruptions();

    verify(disruptionFacade, never()).handleDisruptionDetection(any());
  }

  @Test
  void detectDisruptions_deduplication_mergesIntoExistingDisruption() {
    // Dedup now happens inside DisruptionFacade (find-existing-or-create via sourceReferenceId).
    // The detection service always calls handleDisruptionDetection; the facade returns null when
    // merging with no meaningful change, so count stays 0 but the facade IS invoked.
    TrainStationData late1 = lateTrainAt("CNLY", "E801", 15);
    TrainStationData late2 = lateTrainAt("CNLY", "E802", 22);
    TrainStationData late3 = lateTrainAt("CNLY", "E803", 12);
    when(trainStationDataRepository.findLatestPerStationTrainFromMv())
        .thenThrow(new RuntimeException("MV not available"));
    when(trainStationDataRepository.findLatestPerStationTrain(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of(late1, late2, late3));
    when(trainStationRepository.findByStationCode("CNLY"))
        .thenReturn(new TrainStation(1, "CNLY", "Connolly", null, 53.35, -6.25, null));

    // Facade returns null → merged, no new disruption (no re-notification)
    when(disruptionFacade.handleDisruptionDetection(any())).thenReturn(null);

    service.detectDisruptions();

    verify(disruptionFacade).handleDisruptionDetection(any());
  }

  // ── Congestion detection ───────────────────────────────────────────

  @Test
  void detectDisruptions_highVolumeSiteWithBusRoutes_createsCongestionDisruption() {
    // [site_id, lat, lon, max_volume]
    Object[] row = {"SITE_001", 53.3498, -6.2603, 2000L};
    when(highTrafficPointsRepository.findPeakTrafficSitesFromMv())
        .thenReturn(Collections.singletonList(row));

    // Bus route 46A has a stop within radius
    Object[] routeRow = {"route-46a", "46A"};
    when(busStopRepository.findRouteShortNamesNear(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(Collections.singletonList(routeRow));

    service.detectDisruptions();

    verify(disruptionFacade)
        .handleDisruptionDetection(
            argThat(
                r ->
                    "CONGESTION".equals(r.getDisruptionType())
                        && r.getAffectedTransportModes().contains("BUS")
                        && r.getAffectedRoutes().contains("46A")));
  }

  @Test
  void detectDisruptions_highVolumeSiteNoNearbyTransport_noCongestionDisruption() {
    Object[] row = {"SITE_002", 53.3498, -6.2603, 2000L};
    when(highTrafficPointsRepository.findPeakTrafficSitesWithLocation())
        .thenReturn(Collections.singletonList(row));
    // No bus routes or tram stops nearby — congestion has no transport impact
    when(busStopRepository.findRouteShortNamesNear(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of());
    when(tramStopRepository.findStopsNear(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of());

    service.detectDisruptions();

    verify(disruptionFacade, never()).handleDisruptionDetection(any());
  }

  @Test
  void detectDisruptions_volumeBelowThreshold_noCongestionDisruption() {
    Object[] row = {"SITE_003", 53.3498, -6.2603, 500L}; // below 1500 threshold
    when(highTrafficPointsRepository.findPeakTrafficSitesWithLocation())
        .thenReturn(Collections.singletonList(row));

    service.detectDisruptions();

    verify(disruptionFacade, never()).handleDisruptionDetection(any());
  }

  @Test
  void detectDisruptions_congestionWithTramOnly_warnsTramNoRerouting() {
    Object[] row = {"SITE_004", 53.3382, -6.2591, 1800L};
    when(highTrafficPointsRepository.findPeakTrafficSitesFromMv())
        .thenReturn(Collections.singletonList(row));
    when(busStopRepository.findRouteShortNamesNear(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of()); // no bus routes
    Object[] tramRow = {"STS", "green", "St. Stephen's Green", 53.3382, -6.2591};
    when(tramStopRepository.findStopsNear(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(Collections.singletonList(tramRow));

    service.detectDisruptions();

    verify(disruptionFacade)
        .handleDisruptionDetection(
            argThat(
                r ->
                    "CONGESTION".equals(r.getDisruptionType())
                        && r.getAffectedTransportModes().contains("TRAM")
                        && !r.getAffectedTransportModes().contains("BUS")));
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

  private static <T> T argThat(org.mockito.ArgumentMatcher<T> matcher) {
    return org.mockito.ArgumentMatchers.argThat(matcher);
  }
}
