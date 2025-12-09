package com.trinity.hermes.disruptionmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisruptionService {

    private final DisruptionRepository disruptionRepository;

    public List<DisruptionResponse> getAllDisruptions() {
        return disruptionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Optional<DisruptionResponse> getDisruptionById(Long id) {
        return disruptionRepository.findById(id)
                .map(this::mapToResponse);
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
        return disruptionRepository.findById(id)
                .map(disruption -> {
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
        return new DisruptionResponse(
                disruption.getId(),
                disruption.getName(),
                disruption.getDescription(),
                disruption.getStatus(),
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
