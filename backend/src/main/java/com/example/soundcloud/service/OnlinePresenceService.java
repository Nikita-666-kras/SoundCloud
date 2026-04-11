package com.example.soundcloud.service;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Приблизительный «онлайн»: авторизованные пользователи с запросом за последние 5 минут.
 * In-memory — корректно при одном инстансе бэкенда (типичный VPS).
 */
@Service
public class OnlinePresenceService {

    private static final long WINDOW_MS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<UUID, Long> lastSeenMillis = new ConcurrentHashMap<>();

    public void markPresent(UUID userId) {
        if (userId == null) {
            return;
        }
        lastSeenMillis.put(userId, System.currentTimeMillis());
    }

    public long countOnline() {
        long threshold = System.currentTimeMillis() - WINDOW_MS;
        lastSeenMillis.entrySet().removeIf(e -> e.getValue() < threshold);
        return lastSeenMillis.size();
    }
}
