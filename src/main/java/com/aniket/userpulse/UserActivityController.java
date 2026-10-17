
package com.aniket.userpulse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
        producer.sendEvent(event);
        return "Activity accepted for processing";
    }
}
