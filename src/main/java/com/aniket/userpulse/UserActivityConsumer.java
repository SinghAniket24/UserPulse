package com.aniket.userpulse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserActivityConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(UserActivityConsumer.class);

    @KafkaListener(topics = "user-activity")
    public void consume(UserActivityEvent event) {
        log.info("Received activity event: userId={}, eventType={}",
                event.userId(), event.eventType());
    }
}