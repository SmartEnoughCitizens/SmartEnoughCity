package com.trinity.hermes.notification.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.trinity.hermes.notification.model.Notification;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class SseManager {

    private volatile SseEmitter emitter;

    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    private volatile ScheduledFuture<?> heartbeatTask;

    public synchronized SseEmitter register() {
        if (this.heartbeatTask != null) {
            this.heartbeatTask.cancel(false);
        }

        if (this.emitter != null) {
            try {
                this.emitter.complete();
            } catch (Exception ex) {
                log.warn("Error completing old SSE emitter", ex);
            }
        }

        SseEmitter newEmitter = new SseEmitter(0L);
        this.emitter = newEmitter;

        newEmitter.onCompletion(() -> {
            if (this.emitter == newEmitter) {
                this.emitter = null;
            }
        });

        newEmitter.onTimeout(() -> {
            if (this.emitter == newEmitter) {
                this.emitter = null;
            }
        });

        newEmitter.onError(e -> {
            if (this.emitter == newEmitter) {
                this.emitter = null;
            }
        });

        // Send initial event so browser knows connection is alive
        try {
            newEmitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            log.error("Failed to send initial SSE event", e);
        }

        // Heartbeat every 30s to keep connection alive
        this.heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(() -> {
            SseEmitter current = this.emitter;
            if (current != null) {
                try {
                    current.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    log.warn("Heartbeat failed, clearing emitter");
                    this.emitter = null;
                }
            }
        }, 30, 30, TimeUnit.SECONDS);

        log.info("SSE emitter registered");
        return newEmitter;
    }

    public void push(Notification notification) {
        if (Objects.isNull(emitter)) {
            log.warn("No active SSE emitter; event dropped");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("subject", notification.getSubject());
            payload.put("body", notification.getBody());
            payload.put("recipient", notification.getRecipient());
            payload.put("channel", notification.getChannel() != null ? notification.getChannel().toString() : null);

            if (notification.getQrCode() != null && notification.getQrCode().length > 0) {
                payload.put("qrCode", java.util.Base64.getEncoder().encodeToString(notification.getQrCode()));
            }

            emitter.send(SseEmitter.event().name("notification").data(payload));
            log.info("SSE notification sent: {}", notification.getSubject());
        } catch (IOException ex) {
            log.error("Failed to push SSE event: {}", ex.getMessage());
            emitter = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdownNow();
    }
}