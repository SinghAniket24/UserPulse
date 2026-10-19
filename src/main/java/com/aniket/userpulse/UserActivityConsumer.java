package com.aniket.userpulse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserActivityConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(UserActivityConsumer.class);

    private final ActivityAnalyticsService analyticsService;

    public UserActivityConsumer(ActivityAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @KafkaListener(topics = "user-activity")
    public void consume(UserActivityEvent event) {
        if (event == null) {
            log.warn("Received null activity event");
            return;
        }

        boolean securityAlert = analyticsService.record(event);

        if (securityAlert) {
            log.warn("Security alert: repeated failed logins for userId={}",
                    event.userId());
        }

        log.info("Received activity event: userId={}, eventType={}",
                event.userId(), event.eventType());
    }
}