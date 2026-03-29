package com.trinity.hermes.mv.service;

import com.trinity.hermes.mv.entity.MvRefreshLog;
import com.trinity.hermes.mv.entity.MvRegistry;
import com.trinity.hermes.mv.repository.MvRefreshLogRepository;
import com.trinity.hermes.mv.repository.MvRegistryRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles all transactional DB writes for MV refresh outcomes.
 * Lives in a separate bean so @Transactional is applied via Spring proxy
 * (self-invocation from MaterializedViewService would bypass the proxy).
 */
@Service
@RequiredArgsConstructor
public class MvRefreshLogger {

  private static final int REFRESH_LOG_KEEP_COUNT = 10;

  private final MvRegistryRepository mvRegistryRepository;
  private final MvRefreshLogRepository mvRefreshLogRepository;

  @Transactional
  public void recordResult(MvRegistry registry, String status, long durationMs,
      Instant refreshedAt, String errorMessage, String triggeredBy) {

    registry.setLastRefreshStatus(status);
    registry.setLastRefreshDurationMs(durationMs);
    registry.setLastRefreshedAt(refreshedAt);
    registry.setLastRefreshError(errorMessage);
    mvRegistryRepository.save(registry);

    MvRefreshLog log = MvRefreshLog.builder()
        .mvName(registry.getName())
        .status(status)
        .durationMs(durationMs)
        .refreshedAt(refreshedAt)
        .errorMessage(errorMessage)
        .triggeredBy(triggeredBy)
        .build();
    mvRefreshLogRepository.save(log);
    mvRefreshLogRepository.pruneOldLogs(registry.getName(), REFRESH_LOG_KEEP_COUNT);
  }
}
