package com.aniket.userpulse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
public class UserActivityController {

    private final UserActivityProducer producer;

    public UserActivityController(UserActivityProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String receiveActivity(@RequestBody UserActivityEvent event) {
        if (event == null
                || event.userId() == null || event.userId().isBlank()
                || event.eventType() == null || event.eventType().isBlank()) {
            throw new IllegalArgumentException("Invalid activity request");
        }

        String eventId = (event.eventId() == null || event.eventId().isBlank())
                ? UUID.randomUUID().toString()
                : event.eventId();

        UserActivityEvent eventToSend = new UserActivityEvent(eventId, event.userId(), event.eventType());
        producer.sendEvent(eventToSend);
        return "Activity accepted for processing";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException ex) {
        return "Invalid activity request";
    }
}
