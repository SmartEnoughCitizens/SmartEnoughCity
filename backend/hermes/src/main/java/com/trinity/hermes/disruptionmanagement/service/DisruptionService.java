package com.trinity.hermes.disruptionmanagement.service;

import com.trinity.hermes.disruptionmanagement.dto.CreateDisruptionRequest;
import com.trinity.hermes.disruptionmanagement.dto.DisruptionResponse;
import com.trinity.hermes.disruptionmanagement.dto.UpdateDisruptionRequest;
import com.trinity.hermes.disruptionmanagement.entity.Disruption;
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

  public List<DisruptionResponse> getAllDisruptions() {
    return disruptionRepository.findAll().stream()
        .map(this::mapToResponse)
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

  public DisruptionResponse mapToResponse(Disruption disruption) {
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
        .build();
  }
}
