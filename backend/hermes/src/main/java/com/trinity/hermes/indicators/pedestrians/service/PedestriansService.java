package com.trinity.hermes.indicators.pedestrians.service;

import com.trinity.hermes.indicators.pedestrians.dto.PedestrianLiveDTO;
import com.trinity.hermes.indicators.pedestrians.repository.PedestrianSiteRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedestriansService {

  private final PedestrianSiteRepository pedestrianSiteRepository;

  @Transactional(readOnly = true)
  public List<PedestrianLiveDTO> getLivePedestrianCounts(int limit) {
    log.debug("Fetching live pedestrian counts for up to {} sites", limit);
    List<Object[]> rows = pedestrianSiteRepository.findLatestPedestrianCountsPerSite(limit);
    return rows.stream().map(this::mapToDTO).collect(Collectors.toList());
  }

  private PedestrianLiveDTO mapToDTO(Object[] row) {
    Integer siteId = row[0] != null ? ((Number) row[0]).intValue() : null;
    String siteName = row[1] != null ? row[1].toString() : null;
    Double lat = row[2] != null ? ((Number) row[2]).doubleValue() : null;
    Double lon = row[3] != null ? ((Number) row[3]).doubleValue() : null;
    Long totalCount =
        row[4] != null
            ? (row[4] instanceof BigDecimal ? ((BigDecimal) row[4]).longValue() : ((Number) row[4]).longValue())
            : 0L;
    OffsetDateTime lastUpdated = null;
    if (row[5] != null) {
      if (row[5] instanceof java.sql.Timestamp ts) {
        lastUpdated = ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
      } else if (row[5] instanceof java.time.Instant instant) {
        lastUpdated = instant.atOffset(java.time.ZoneOffset.UTC);
      } else if (row[5] instanceof OffsetDateTime odt) {
        lastUpdated = odt;
      }
    }

    return PedestrianLiveDTO.builder()
        .siteId(siteId)
        .siteName(siteName)
        .lat(lat)
        .lon(lon)
        .totalCount(totalCount)
        .lastUpdated(lastUpdated)
        .build();
  }
}
