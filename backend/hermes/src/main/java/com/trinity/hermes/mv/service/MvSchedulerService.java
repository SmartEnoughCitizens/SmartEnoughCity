package com.trinity.hermes.mv.service;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.mv.entity.MvRegistry;
import com.trinity.hermes.mv.repository.MvRegistryRepository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Manages dynamic per-MV cron schedules loaded from mv_registry at startup, and updated live
 * whenever an MV is upserted or toggled without requiring a restart.
 */
@Service
@Slf4j
public class MvSchedulerService implements SchedulingConfigurer {

  private final MvRegistryRepository mvRegistryRepository;
  private final TaskScheduler mvTaskScheduler;
  private final ApplicationContext applicationContext;

  /** Holds the live ScheduledFuture per MV name so we can cancel/reschedule them. */
  private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

  public MvSchedulerService(
      MvRegistryRepository mvRegistryRepository,
      @Qualifier("mvTaskScheduler") TaskScheduler mvTaskScheduler,
      ApplicationContext applicationContext) {
    this.mvRegistryRepository = mvRegistryRepository;
    this.mvTaskScheduler = mvTaskScheduler;
    this.applicationContext = applicationContext;
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    registrar.setTaskScheduler(mvTaskScheduler);

    // Schedule all enabled MVs that have a cron expression at startup.
    // Wrapped in try-catch so the app starts cleanly on a fresh DB (mv_registry not yet created).
    try {
      mvRegistryRepository.findAllByEnabledTrue().stream()
          .filter(mv -> StringUtils.hasText(mv.getRefreshCron()))
          .forEach(this::scheduleInternal);
      log.info(
          "MvSchedulerService: scheduled {} MV refresh tasks at startup", scheduledTasks.size());
    } catch (Exception e) {
      log.warn(
          "MvSchedulerService: could not load schedules at startup (mv_registry may not exist yet): {}",
          e.getMessage());
    }
  }

  /**
   * Called by MaterializedViewService after an upsert. Cancels any existing schedule for this MV
   * and creates a new one if cron is set.
   */
  public void reschedule(MvRegistry mv) {
    cancel(mv.getName());
    if (mv.isEnabled() && StringUtils.hasText(mv.getRefreshCron())) {
      scheduleInternal(mv);
      log.info("Rescheduled MV '{}' with cron '{}'", mv.getName(), mv.getRefreshCron());
    }
  }

  /** Cancels the scheduled task for a named MV (on drop or disable). */
  public void cancel(String name) {
    ScheduledFuture<?> existing = scheduledTasks.remove(name);
    if (existing != null) {
      existing.cancel(false);
      log.info("Cancelled scheduled refresh for MV '{}'", LogSanitizer.sanitizeLog(name));
    }
  }

  private void scheduleInternal(MvRegistry mv) {
    CronTrigger trigger = new CronTrigger(mv.getRefreshCron());
    // Lazy-fetch MaterializedViewService via context to avoid circular dependency
    // (MaterializedViewService → MvSchedulerService → MaterializedViewService)
    ScheduledFuture<?> future =
        mvTaskScheduler.schedule(
            () ->
                applicationContext
                    .getBean(MaterializedViewService.class)
                    .refresh(mv.getName(), "SCHEDULER"),
            trigger);
    scheduledTasks.put(mv.getName(), future);
    log.debug("Scheduled MV '{}' with cron '{}'", mv.getName(), mv.getRefreshCron());
  }
}
