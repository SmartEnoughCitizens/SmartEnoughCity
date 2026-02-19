package com.trinity.hermes.notification.util;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.trinity.hermes.notification.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class SseManager {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String userId) {
        SseEmitter old = emitters.remove(userId);
        if (old != null) {
            try {
                old.complete();
            } catch (Exception ex) {
                log.warn("Error completing old SSE emitter for user {}", userId, ex);
            }
        }

        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(e -> emitters.remove(userId, emitter));

        log.info("SSE emitter registered for user {}", userId);
        return emitter;
    }

    public void push(String userId, Notification notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.warn("No active SSE emitter for user {}; event dropped", userId);
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("subject", notification.getSubject());
            payload.put("body", notification.getBody());
            payload.put("recipient", notification.getRecipient());
            payload.put("channel", notification.getChannel() != null ? notification.getChannel().toString() : null);
            // TODO: Add type and priority fields to Notification model and include here

            if (notification.getQrCode() != null && notification.getQrCode().length > 0) {
                payload.put("qrCode", Base64.getEncoder().encodeToString(notification.getQrCode()));
            }

            emitter.send(SseEmitter.event().name("notification").data(payload));
            log.info("SSE notification sent to user {}: {}", userId, notification.getSubject());
        } catch (IOException ex) {
            log.error("Failed to push SSE event to user {}: {}", userId, ex.getMessage());
            emitters.remove(userId, emitter);
        }
    }
}
