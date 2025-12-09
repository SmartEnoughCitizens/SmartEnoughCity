package com.trinity.hermes.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
public class SseManager {

    /** The single global stream emitter */
    private volatile SseEmitter emitter;

    /**
     * Register a single SSE emitter.
     * Any new connection replaces the previous one.
     */
    public synchronized SseEmitter register() {
        // Close old emitter if present
        if (this.emitter != null) {
            try {
                this.emitter.complete();
            } catch (Exception ignored) {}
        }

        SseEmitter newEmitter = new SseEmitter(0L);
        this.emitter = newEmitter;

        log.info("Global SSE emitter registered");

        newEmitter.onCompletion(() -> {
            log.info("Global SSE completed");
            this.emitter = null;
        });

        newEmitter.onTimeout(() -> {
            log.warn("Global SSE timeout");
            this.emitter = null;
        });

        newEmitter.onError((e) -> {
            log.error("Global SSE error: {}", e.getMessage());
            this.emitter = null;
        });

        return newEmitter;
    }

    /**
     * Push an event to the global SSE stream.
     */
    public void push(Object data) {
        if (Objects.isNull(emitter)) {
            log.warn("No active SSE emitter; event dropped");
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("notification")
                            .data(data)
            );
        } catch (IOException ex) {
            log.error("Failed to push SSE event: {}", ex.getMessage());
            emitter = null;
        }
    }
}