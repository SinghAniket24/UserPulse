package com.aniket.userpulse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ActivityAnalyticsService {

    private static final Logger log =
            LoggerFactory.getLogger(ActivityAnalyticsService.class);

    private static final int FAILED_LOGIN_THRESHOLD = 3;

    private final ActivityEventRepository repository;

    private final ConcurrentHashMap<String, AtomicInteger> failedLoginAttempts =
            new ConcurrentHashMap<>();

    public ActivityAnalyticsService(ActivityEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean record(UserActivityEvent event) {
        if (event == null || event.eventType() == null || event.eventType().isBlank()) {
            throw new IllegalArgumentException("Event and eventType must not be null or blank");
        }

        int rowsAffected = repository.insertIfNotExists(event.eventId(), event.userId(), event.eventType());
        if (rowsAffected == 0) {
            log.info("Duplicate activity event ignored: eventId={}, userId={}, eventType={}",
                    event.eventId(), event.userId(), event.eventType());
            return false;
        }

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
        List<Object[]> results = repository.findEventTypeCounts();
        if (results == null || results.isEmpty()) {
            return Collections.emptyMap();
        }

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }
}
