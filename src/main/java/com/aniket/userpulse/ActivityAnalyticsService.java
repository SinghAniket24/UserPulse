package com.aniket.userpulse;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Service
public class ActivityAnalyticsService {

    private static final int FAILED_LOGIN_THRESHOLD = 3;

    private final ConcurrentHashMap<String, LongAdder> counts =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, AtomicInteger> failedLoginAttempts =
            new ConcurrentHashMap<>();

    public boolean record(UserActivityEvent event) {
        counts.computeIfAbsent(event.eventType(), key -> new LongAdder())
                .increment();

        if ("LOGIN_FAILED".equals(event.eventType())
                && event.userId() != null
                && !event.userId().isBlank()) {

            int attempts = failedLoginAttempts
                    .computeIfAbsent(event.userId(), key -> new AtomicInteger())
                    .incrementAndGet();

            return attempts == FAILED_LOGIN_THRESHOLD;
        }

        if ("LOGIN_SUCCESS".equals(event.eventType())
                && event.userId() != null) {
            failedLoginAttempts.remove(event.userId());
        }

        return false;
    }

    public Map<String, Long> getCounts() {
        return counts.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().sum()
                ));
    }
}