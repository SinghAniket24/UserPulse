package com.aniket.userpulse;

public record UserActivityEvent(
        String userId,
        String eventType
) {
}