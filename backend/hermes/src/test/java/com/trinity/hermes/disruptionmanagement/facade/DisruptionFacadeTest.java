package com.trinity.hermes.disruptionmanagement.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trinity.hermes.disruptionmanagement.dto.DisruptionSolution;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.entity.DisruptionAlternative;
import com.trinity.hermes.disruptionmanagement.entity.DisruptionCause;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionAlternativeRepository;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionCauseRepository;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionRepository;
import com.trinity.hermes.disruptionmanagement.service.AlternativeTransportService;
import com.trinity.hermes.disruptionmanagement.service.CauseCorrelationService;
import com.trinity.hermes.disruptionmanagement.service.DisruptionService;
import com.trinity.hermes.disruptionmanagement.service.IncidentLoggingService;
import com.trinity.hermes.disruptionmanagement.service.ThresholdDetectionService;
import com.trinity.hermes.notification.services.NotificationFacade;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("UnusedVariable") // @Mock fields injected via @InjectMocks — not read directly
class DisruptionFacadeTest {

  @Mock private DisruptionService disruptionService;
  @Mock private ThresholdDetectionService thresholdDetectionService;
  @Mock private NotificationFacade notificationFacade;
  @Mock private IncidentLoggingService incidentLoggingService;
  @Mock private CauseCorrelationService causeCorrelationService;
  @Mock private AlternativeTransportService alternativeTransportService;
  @Mock private UserManagementService userManagementService;
  @Mock private DisruptionRepository disruptionRepository;
  @Mock private DisruptionCauseRepository disruptionCauseRepository;
  @Mock private DisruptionAlternativeRepository disruptionAlternativeRepository;

  @InjectMocks private DisruptionFacade facade;

  @BeforeEach
  void setUp() {
    when(causeCorrelationService.correlateCauses(any())).thenReturn(List.of());
    when(alternativeTransportService.getAlternatives(any())).thenReturn(List.of());
    when(userManagementService.getUsersByRole(anyString())).thenReturn(List.of());
  }

  // ── Cause persistence ───────────────────────────────────────────────

  @Test
  void processDisruption_withCauses_savesAllCauses() {
    Disruption disruption = busDisruption();
    DisruptionCause cause =
        DisruptionCause.builder()
            .disruption(disruption)
            .causeType("EVENT")
            .causeDescription("Test")
            .confidence("HIGH")
            .build();
    when(causeCorrelationService.correlateCauses(disruption)).thenReturn(List.of(cause));

    facade.processDisruption(disruption);

    verify(disruptionCauseRepository).saveAll(List.of(cause));
  }

  @Test
  void processDisruption_noCauses_doesNotSave() {
    Disruption disruption = busDisruption();
    when(causeCorrelationService.correlateCauses(disruption)).thenReturn(List.of());

    facade.processDisruption(disruption);

    verify(disruptionCauseRepository, never()).saveAll(anyList());
  }

  // ── Alternative persistence ─────────────────────────────────────────

  @Test
  void processDisruption_withAlternatives_savesAllAlternatives() {
    Disruption disruption = busDisruption();
    DisruptionAlternative alt =
        DisruptionAlternative.builder()
            .disruption(disruption)
            .mode("bus")
            .description("Nearby bus stop")
            .build();
    when(alternativeTransportService.getAlternatives(disruption)).thenReturn(List.of(alt));

    facade.processDisruption(disruption);

    verify(disruptionAlternativeRepository).saveAll(List.of(alt));
  }

  @Test
  void processDisruption_noAlternatives_doesNotSave() {
    Disruption disruption = busDisruption();
    when(alternativeTransportService.getAlternatives(disruption)).thenReturn(List.of());

    facade.processDisruption(disruption);

    verify(disruptionAlternativeRepository, never()).saveAll(anyList());
  }

  @Test
  void processDisruption_alternativesPopulateSolutionRoutes() {
    Disruption disruption = busDisruption();
    DisruptionAlternative alt =
        DisruptionAlternative.builder()
            .disruption(disruption)
            .mode("bus")
            .description("Bus stop: Collins Ave (150m away)")
            .build();
    when(alternativeTransportService.getAlternatives(disruption)).thenReturn(List.of(alt));

    DisruptionSolution solution = facade.processDisruption(disruption);

    assertThat(solution.getAlternativeRoutes()).contains("Bus stop: Collins Ave (150m away)");
    assertThat(solution.getPrimaryRecommendation()).isEqualTo("Bus stop: Collins Ave (150m away)");
  }

  // ── affectedUserGroups by transport mode ───────────────────────────

  @Test
  void processDisruption_busMode_notifiesBusRoles() {
    Disruption disruption = disruptionWithModes("BUS");
    UserRepresentation admin = mockUser("admin-id");
    UserRepresentation provider = mockUser("provider-id");
    UserRepresentation manager = mockUser("manager-id");
    when(userManagementService.getUsersByRole("Bus_Admin")).thenReturn(List.of(admin));
    when(userManagementService.getUsersByRole("Bus_Provider")).thenReturn(List.of(provider));
    when(userManagementService.getUsersByRole("City_Manager")).thenReturn(List.of(manager));

    DisruptionSolution solution = facade.processDisruption(disruption);

    assertThat(solution.getAffectedUserGroups())
        .containsExactlyInAnyOrder("admin-id", "provider-id", "manager-id");
  }

  @Test
  void processDisruption_trainMode_notifiesTrainRoles() {
    Disruption disruption = disruptionWithModes("TRAIN");
    when(userManagementService.getUsersByRole("Train_Admin")).thenReturn(List.of(mockUser("ta")));
    when(userManagementService.getUsersByRole("Train_Provider"))
        .thenReturn(List.of(mockUser("tp")));
    when(userManagementService.getUsersByRole("City_Manager")).thenReturn(List.of(mockUser("cm")));

    DisruptionSolution solution = facade.processDisruption(disruption);

    assertThat(solution.getAffectedUserGroups()).containsExactlyInAnyOrder("ta", "tp", "cm");
  }

  @Test
  void processDisruption_tramMode_notifiesTramRoles() {
    Disruption disruption = disruptionWithModes("TRAM");
    when(userManagementService.getUsersByRole("Tram_Admin")).thenReturn(List.of(mockUser("tma")));
    when(userManagementService.getUsersByRole("Tram_Provider"))
        .thenReturn(List.of(mockUser("tmp")));
    when(userManagementService.getUsersByRole("City_Manager")).thenReturn(List.of(mockUser("cm")));

    DisruptionSolution solution = facade.processDisruption(disruption);

    assertThat(solution.getAffectedUserGroups()).containsExactlyInAnyOrder("tma", "tmp", "cm");
  }

  @Test
  void processDisruption_unknownMode_notifiesCityManagerOnly() {
    Disruption disruption = disruptionWithModes("CAR");
    when(userManagementService.getUsersByRole("City_Manager")).thenReturn(List.of(mockUser("cm")));

    DisruptionSolution solution = facade.processDisruption(disruption);

    assertThat(solution.getAffectedUserGroups()).containsExactly("cm");
  }

  // ── DTO mapping ─────────────────────────────────────────────────────

  @Test
  void processDisruption_solutionContainsCoreFields() {
    Disruption disruption = busDisruption();

    DisruptionSolution solution = facade.processDisruption(disruption);

    assertThat(solution.getDisruptionId()).isEqualTo(1L);
    assertThat(solution.getSeverity()).isEqualTo("MEDIUM");
    assertThat(solution.getAffectedArea()).isEqualTo("Test Area");
  }

  // ── Helpers ─────────────────────────────────────────────────────────

  private Disruption busDisruption() {
    return disruptionWithModes("BUS");
  }

  private Disruption disruptionWithModes(String... modes) {
    Disruption d = new Disruption();
    d.setId(1L);
    d.setName("Test disruption");
    d.setDisruptionType("DELAY");
    d.setSeverity("MEDIUM");
    d.setAffectedArea("Test Area");
    d.setAffectedTransportModes(List.of(modes));
    d.setStatus("ANALYZING");
    return d;
  }

  private UserRepresentation mockUser(String id) {
    UserRepresentation u = new UserRepresentation();
    u.setId(id);
    return u;
  }
}
