package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.dto.AlternativeDTO;
import com.trinity.hermes.disruptionmanagement.dto.CauseDTO;
import com.trinity.hermes.disruptionmanagement.dto.CreateDisruptionRequest;
import com.trinity.hermes.disruptionmanagement.dto.DisruptionResponse;
import com.trinity.hermes.disruptionmanagement.dto.UpdateDisruptionRequest;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionAlternativeRepository;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionCauseRepository;
import com.trinity.hermes.disruptionmanagement.repository.DisruptionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DisruptionService {

  private final DisruptionRepository disruptionRepository;
  private final DisruptionCauseRepository disruptionCauseRepository;
  private final DisruptionAlternativeRepository disruptionAlternativeRepository;

  public List<DisruptionResponse> getAllDisruptions() {
    return disruptionRepository.findAll().stream()
        .map(this::mapToSummary)
        .collect(Collectors.toList());
  }

  public Optional<DisruptionResponse> getDisruptionById(Long id) {
    return disruptionRepository.findById(id).map(this::mapToResponse);
  }

  public DisruptionResponse createDisruption(CreateDisruptionRequest request) {
    Disruption disruption = new Disruption();
    disruption.setName(request.getName());
    disruption.setDescription(request.getDescription());
    disruption.setStatus("PENDING");
    Disruption saved = disruptionRepository.save(disruption);
    return mapToResponse(saved);
  }

  public Optional<DisruptionResponse> updateDisruption(Long id, UpdateDisruptionRequest request) {
    return disruptionRepository
        .findById(id)
        .map(
            disruption -> {
              disruption.setName(request.getName());
              disruption.setDescription(request.getDescription());
              disruption.setStatus(request.getStatus());
              return mapToResponse(disruptionRepository.save(disruption));
            });
  }

  public boolean deleteDisruption(Long id) {
    if (disruptionRepository.existsById(id)) {
      disruptionRepository.deleteById(id);
      return true;
    }
    return false;
  }

  /** Lightweight summary used for list endpoints — no N+1 causes/alternatives queries. */
  public DisruptionResponse mapToSummary(Disruption disruption) {
    LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Europe/Dublin"));
    return DisruptionResponse.builder()
        .id(disruption.getId())
        .name(disruption.getName())
        .description(disruption.getDescription())
        .status(disruption.getStatus())
        .severity(disruption.getSeverity())
        .disruptionType(disruption.getDisruptionType())
        .affectedTransportModes(disruption.getAffectedTransportModes())
        .affectedRoutes(disruption.getAffectedRoutes())
        .affectedArea(disruption.getAffectedArea())
        .latitude(disruption.getLatitude())
        .longitude(disruption.getLongitude())
        .detectedAt(disruption.getDetectedAt())
        .estimatedEndTime(disruption.getEstimatedEndTime())
        .delayMinutes(disruption.getDelayMinutes())
        .notificationSent(disruption.getNotificationSent())
        .createdAt(disruption.getDetectedAt() != null ? disruption.getDetectedAt() : now)
        .updatedAt(now)
        .causes(List.of())
        .alternatives(List.of())
        .build();
  }

  public DisruptionResponse mapToResponse(Disruption disruption) {
    LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Europe/Dublin"));

    List<CauseDTO> causes =
        disruption.getId() != null
            ? disruptionCauseRepository.findByDisruptionId(disruption.getId()).stream()
                .map(
                    c ->
                        CauseDTO.builder()
                            .id(c.getId())
                            .causeType(c.getCauseType())
                            .causeDescription(c.getCauseDescription())
                            .confidence(c.getConfidence())
                            .build())
                .collect(Collectors.toList())
            : List.of();

    Double disruptionLat = disruption.getLatitude();
    Double disruptionLon = disruption.getLongitude();

    List<AlternativeDTO> alternatives =
        disruption.getId() != null
            ? disruptionAlternativeRepository.findByDisruptionId(disruption.getId()).stream()
                .map(
                    a ->
                        AlternativeDTO.builder()
                            .id(a.getId())
                            .mode(a.getMode())
                            .description(a.getDescription())
                            .etaMinutes(a.getEtaMinutes())
                            .stopName(a.getStopName())
                            .availabilityCount(a.getAvailabilityCount())
                            .lat(a.getLat())
                            .lon(a.getLon())
                            .googleMapsWalkingUrl(
                                buildGoogleMapsWalkingUrl(
                                    disruptionLat, disruptionLon, a.getLat(), a.getLon()))
                            .build())
                .collect(Collectors.toList())
            : List.of();

    return DisruptionResponse.builder()
        .id(disruption.getId())
        .name(disruption.getName())
        .description(disruption.getDescription())
        .status(disruption.getStatus())
        .severity(disruption.getSeverity())
        .disruptionType(disruption.getDisruptionType())
        .affectedTransportModes(disruption.getAffectedTransportModes())
        .affectedRoutes(disruption.getAffectedRoutes())
        .affectedArea(disruption.getAffectedArea())
        .latitude(disruption.getLatitude())
        .longitude(disruption.getLongitude())
        .detectedAt(disruption.getDetectedAt())
        .estimatedEndTime(disruption.getEstimatedEndTime())
        .delayMinutes(disruption.getDelayMinutes())
        .notificationSent(disruption.getNotificationSent())
        .createdAt(disruption.getDetectedAt() != null ? disruption.getDetectedAt() : now)
        .updatedAt(now)
        .causes(causes)
        .alternatives(alternatives)
        .build();
  }

  private String buildGoogleMapsWalkingUrl(
      Double fromLat, Double fromLon, Double toLat, Double toLon) {
    if (fromLat == null || fromLon == null || toLat == null || toLon == null) return null;
    return String.format(
        "https://www.google.com/maps/dir/?api=1&origin=%s,%s&destination=%s,%s&travelmode=walking",
        fromLat, fromLon, toLat, toLon);
  }
}
