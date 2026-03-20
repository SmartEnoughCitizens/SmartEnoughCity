package com.trinity.hermes.indicators.pedestrians.facade;

import com.trinity.hermes.indicators.pedestrians.dto.PedestrianLiveDTO;
import com.trinity.hermes.indicators.pedestrians.service.PedestriansService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedestriansFacade {

  private final PedestriansService pedestriansService;

  public List<PedestrianLiveDTO> getLiveCounts(int limit) {
    return pedestriansService.getLivePedestrianCounts(limit);
  }
}
