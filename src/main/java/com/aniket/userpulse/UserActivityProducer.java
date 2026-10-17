
package com.aniket.userpulse;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserActivityProducer {

    private final KafkaTemplate<String, UserActivityEvent> kafkaTemplate;

    public UserActivityProducer(
            KafkaTemplate<String, UserActivityEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(UserActivityEvent event) {
        kafkaTemplate.send("user-activity", event.userId(), event);
    }
}
