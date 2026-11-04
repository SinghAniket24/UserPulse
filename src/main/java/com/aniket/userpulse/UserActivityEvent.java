package com.aniket.userpulse;

public record UserActivityEvent(
        String eventId,
        String userId,
        String eventType
) {
    public UserActivityEvent(String userId, String eventType) {
        this(null, userId, eventType);
    }
}
