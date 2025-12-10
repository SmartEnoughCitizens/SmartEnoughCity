package com.trinity.hermes.notification.util;

import com.trinity.hermes.notification.model.Notification;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Component
public class InMemoryNotificationStore {

    private static final int MAX_SIZE = 10;

    // Thread-safe list wrapper
    private final List<Notification> notifications =
            Collections.synchronizedList(new LinkedList<>());

    /** Add new notification, remove oldest if we exceed MAX_SIZE */
    public void add(Notification notification) {
        synchronized (notifications) {
            notifications.add(notification);
            if (notifications.size() > MAX_SIZE) {
                notifications.remove(0); // remove oldest
            }
        }
    }

    /** Returns a snapshot (copy) of all notifications */
    public List<Notification> getAll() {
        synchronized (notifications) {
            return List.copyOf(notifications);
        }
    }

    /** Returns latest notification or null */
    public Optional<Notification> getLatest() {
        synchronized (notifications) {
            if (notifications.isEmpty()) return null;
            return Optional.ofNullable(notifications.get(notifications.size() - 1));
        }
    }

    /** Clear the in-memory store */
    public void clear() {
        synchronized (notifications) {
            notifications.clear();
        }
    }
}