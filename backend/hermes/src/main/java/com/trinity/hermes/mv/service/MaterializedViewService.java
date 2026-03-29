package com.trinity.hermes.mv.service;

import com.trinity.hermes.mv.dto.MvRefreshResult;
import com.trinity.hermes.mv.dto.MvRegistryDTO;
import com.trinity.hermes.mv.dto.UpsertMvRequest;
import com.trinity.hermes.mv.entity.MvRegistry;
import com.trinity.hermes.mv.repository.MvRefreshLogRepository;
import com.trinity.hermes.mv.repository.MvRegistryRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterializedViewService {

  private static final String DEFAULT_SCHEMA = "backend";

  private final MvRegistryRepository mvRegistryRepository;
  private final MvRefreshLogRepository mvRefreshLogRepository;
  private final JdbcTemplate jdbcTemplate;
  private final MvSchedulerService mvSchedulerService;
  private final MvRefreshLogger mvRefreshLogger;

  // ── Upsert ────────────────────────────────────────────────────────────────

  @Transactional
  public MvRegistryDTO upsert(UpsertMvRequest request) {
    validateRequest(request);

    String schema = StringUtils.hasText(request.getViewSchema()) ? request.getViewSchema() : DEFAULT_SCHEMA;
    boolean isUpdate = mvRegistryRepository.existsByName(request.getName());

    MvRegistry registry = mvRegistryRepository.findByName(request.getName())
        .orElseGet(() -> MvRegistry.builder()
            .name(request.getName())
            .version(1)
            .build());

    if (isUpdate) {
      log.info("Updating MV '{}' — dropping existing view", request.getName());
      jdbcTemplate.execute(String.format("DROP MATERIALIZED VIEW IF EXISTS %s.%s CASCADE", schema, request.getName()));
      registry.setVersion(registry.getVersion() + 1);
    }

    registry.setDescription(request.getDescription());
    registry.setViewSchema(schema);
    registry.setQuerySql(request.getQuerySql());
    registry.setUniqueKeyColumns(request.getUniqueKeyColumns());
    registry.setRefreshCron(StringUtils.hasText(request.getRefreshCron()) ? request.getRefreshCron() : null);
    registry.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

    String createSql = String.format("CREATE MATERIALIZED VIEW %s.%s AS %s", schema, request.getName(), request.getQuerySql());
    log.info("Creating MV '{}' in schema '{}'", request.getName(), schema);
    jdbcTemplate.execute(createSql);

    // Create unique indexes required for REFRESH MATERIALIZED VIEW CONCURRENTLY
    Arrays.stream(request.getUniqueKeyColumns().split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .forEach(col -> {
          String idxName = String.format("%s_%s_idx", request.getName(), col);
          String idxSql = String.format(
              "CREATE UNIQUE INDEX IF NOT EXISTS %s ON %s.%s (%s)", idxName, schema, request.getName(), col);
          log.debug("Creating unique index: {}", idxName);
          jdbcTemplate.execute(idxSql);
        });

    MvRegistry saved = mvRegistryRepository.save(registry);
    mvSchedulerService.reschedule(saved);

    log.info("{} MV '{}' (version {})", isUpdate ? "Updated" : "Registered", saved.getName(), saved.getVersion());
    return toDTO(saved);
  }

  // ── Refresh ───────────────────────────────────────────────────────────────

  /**
   * Refreshes a named MV via REFRESH MATERIALIZED VIEW CONCURRENTLY.
   * Must NOT run inside a transaction — Postgres disallows REFRESH CONCURRENTLY in a tx block.
   * Concurrent refreshes of the same MV are serialized by Postgres itself via an exclusive lock.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public MvRefreshResult refresh(String name) {
    return refresh(name, "API");
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public MvRefreshResult refresh(String name, String triggeredBy) {
    MvRegistry registry = mvRegistryRepository.findByName(name)
        .orElseThrow(() -> new NoSuchElementException("MV not found: " + name));

    long start = System.currentTimeMillis();
    Instant refreshedAt = Instant.now();
    String status;
    String errorMessage = null;

    try {
      String sql = String.format("REFRESH MATERIALIZED VIEW CONCURRENTLY %s.%s", registry.getViewSchema(), name);
      log.info("Refreshing MV '{}' (triggered by: {})", name, triggeredBy);
      jdbcTemplate.execute(sql);

      long durationMs = System.currentTimeMillis() - start;
      status = "SUCCESS";
      mvRefreshLogger.recordResult(registry, status, durationMs, refreshedAt, null, triggeredBy);
      log.info("Refreshed MV '{}' in {}ms", name, durationMs);
      return MvRefreshResult.builder()
          .mvName(name).status(status).durationMs(durationMs).refreshedAt(refreshedAt).build();

    } catch (Exception e) {
      long durationMs = System.currentTimeMillis() - start;
      status = "FAILED";
      errorMessage = e.getMessage();
      log.error("Failed to refresh MV '{}': {}", name, errorMessage);
      mvRefreshLogger.recordResult(registry, status, durationMs, refreshedAt, errorMessage, triggeredBy);
      return MvRefreshResult.builder()
          .mvName(name).status(status).durationMs(durationMs).refreshedAt(refreshedAt)
          .errorMessage(errorMessage).build();
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public List<MvRefreshResult> refreshAll() {
    return mvRegistryRepository.findAllByEnabledTrue().stream()
        .map(mv -> refresh(mv.getName(), "REFRESH_ALL"))
        .collect(Collectors.toList());
  }

  // ── Drop ──────────────────────────────────────────────────────────────────

  @Transactional
  public void drop(String name) {
    MvRegistry registry = mvRegistryRepository.findByName(name)
        .orElseThrow(() -> new NoSuchElementException("MV not found: " + name));

    mvSchedulerService.cancel(name);
    jdbcTemplate.execute(String.format("DROP MATERIALIZED VIEW IF EXISTS %s.%s CASCADE",
        registry.getViewSchema(), name));
    mvRefreshLogRepository.deleteByMvName(name);
    mvRegistryRepository.delete(registry);
    log.info("Dropped MV '{}' and removed from registry", name);
  }

  // ── Toggle enabled ────────────────────────────────────────────────────────

  @Transactional
  public MvRegistryDTO toggle(String name) {
    MvRegistry registry = mvRegistryRepository.findByName(name)
        .orElseThrow(() -> new NoSuchElementException("MV not found: " + name));
    registry.setEnabled(!registry.isEnabled());
    MvRegistry saved = mvRegistryRepository.save(registry);
    if (!saved.isEnabled()) {
      mvSchedulerService.cancel(name);
    } else {
      mvSchedulerService.reschedule(saved);
    }
    return toDTO(saved);
  }

  // ── Queries ───────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<MvRegistryDTO> findAll() {
    return mvRegistryRepository.findAll().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public MvRegistryDTO findByName(String name) {
    return mvRegistryRepository.findByName(name)
        .map(this::toDTO)
        .orElseThrow(() -> new NoSuchElementException("MV not found: " + name));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private void validateRequest(UpsertMvRequest request) {
    if (request.getQuerySql().contains(";")) {
      throw new IllegalArgumentException("querySql must not contain semicolons");
    }
    if (StringUtils.hasText(request.getRefreshCron()) && !CronExpression.isValidExpression(request.getRefreshCron())) {
      throw new IllegalArgumentException("Invalid cron expression: " + request.getRefreshCron());
    }
    if (!request.getUniqueKeyColumns().matches("^[a-z][a-z0-9_]*(,\\s*[a-z][a-z0-9_]*)*$")) {
      throw new IllegalArgumentException("uniqueKeyColumns must be comma-separated lowercase column names");
    }
  }

  private MvRegistryDTO toDTO(MvRegistry r) {
    List<MvRefreshResult> history = mvRefreshLogRepository
        .findByMvNameOrderByRefreshedAtDesc(r.getName())
        .stream()
        .map(l -> MvRefreshResult.builder()
            .mvName(l.getMvName())
            .status(l.getStatus())
            .durationMs(l.getDurationMs())
            .refreshedAt(l.getRefreshedAt())
            .errorMessage(l.getErrorMessage())
            .build())
        .collect(Collectors.toList());

    return MvRegistryDTO.builder()
        .id(r.getId())
        .name(r.getName())
        .description(r.getDescription())
        .viewSchema(r.getViewSchema())
        .querySql(r.getQuerySql())
        .uniqueKeyColumns(r.getUniqueKeyColumns())
        .refreshCron(r.getRefreshCron())
        .enabled(r.isEnabled())
        .version(r.getVersion())
        .createdAt(r.getCreatedAt())
        .updatedAt(r.getUpdatedAt())
        .lastRefreshedAt(r.getLastRefreshedAt())
        .lastRefreshDurationMs(r.getLastRefreshDurationMs())
        .lastRefreshStatus(r.getLastRefreshStatus())
        .lastRefreshError(r.getLastRefreshError())
        .refreshHistory(history)
        .build();
  }
}
