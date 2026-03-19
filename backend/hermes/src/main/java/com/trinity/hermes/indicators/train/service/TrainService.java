package com.trinity.hermes.indicators.train.service;

import com.trinity.hermes.indicators.train.dto.TrainDTO;
import com.trinity.hermes.indicators.train.entity.TrainStation;
import com.trinity.hermes.indicators.train.repository.TrainStationRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainService {

  private final TrainStationRepository trainStationRepository;

  @Transactional(readOnly = true)
  public List<TrainDTO> getAllTrainStations(int limit) {
    log.debug("Fetching up to {} train stations from database", limit);
    return trainStationRepository.findAll(PageRequest.of(0, limit)).stream()
        .map(this::mapToDTO)
        .collect(Collectors.toList());
  }

  private TrainDTO mapToDTO(TrainStation entity) {
    TrainDTO dto = new TrainDTO();
    dto.setId(entity.getId());
    dto.setStationCode(entity.getStationCode());
    dto.setStationDesc(entity.getStationDesc());
    dto.setStationAlias(entity.getStationAlias());
    dto.setLat(entity.getLat());
    dto.setLon(entity.getLon());
    dto.setStationType(entity.getStationType());
    return dto;
  }
}
