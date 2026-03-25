package com.trinity.hermes.indicators.tram.service;

import com.trinity.hermes.indicators.tram.dto.TramStopDTO;
import com.trinity.hermes.indicators.tram.entity.TramStop;
import com.trinity.hermes.indicators.tram.repository.TramStopRepository;
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
public class TramService {

  private final TramStopRepository tramStopRepository;

  @Transactional(readOnly = true)
  public List<TramStopDTO> getAllStops(int limit) {
    log.debug("Fetching up to {} tram stops from database", limit);
    return tramStopRepository.findAll(PageRequest.of(0, limit)).stream()
        .map(this::mapToDTO)
        .collect(Collectors.toList());
  }

  private TramStopDTO mapToDTO(TramStop entity) {
    TramStopDTO dto = new TramStopDTO();
    dto.setStopId(entity.getStopId());
    dto.setLine(entity.getLine());
    dto.setName(entity.getName());
    dto.setLat(entity.getLat());
    dto.setLon(entity.getLon());
    dto.setParkRide(entity.getParkRide());
    dto.setCycleRide(entity.getCycleRide());
    return dto;
  }
}
