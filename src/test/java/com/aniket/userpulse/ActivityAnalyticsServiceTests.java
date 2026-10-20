package com.aniket.userpulse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityAnalyticsServiceTests {

    private ActivityAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new ActivityAnalyticsService();
    }

    @Test
    void shouldCountRepeatedEvents() {
        analyticsService.record(new UserActivityEvent("user-1", "LOGIN_SUCCESS"));
        analyticsService.record(new UserActivityEvent("user-2", "LOGIN_SUCCESS"));

        assertEquals(2L, analyticsService.getCounts().get("LOGIN_SUCCESS"));
    }

    @Test
    void shouldTrackDifferentEventTypesSeparately() {
        analyticsService.record(new UserActivityEvent("user-1", "LOGIN_SUCCESS"));
        analyticsService.record(new UserActivityEvent("user-2", "USER_SIGNED_UP"));

        assertEquals(
                Map.of("LOGIN_SUCCESS", 1L, "USER_SIGNED_UP", 1L),
                analyticsService.getCounts());
    }

    @Test
    void shouldReturnEmptyCountsInitially() {
        assertTrue(analyticsService.getCounts().isEmpty());
    }

    @Test
    void shouldAlertOnThirdFailedLogin() {
        assertFalse(analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED")));
        assertFalse(analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED")));
        assertTrue(analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED")));
        assertFalse(analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED")));
    }

    @Test
    void shouldResetFailedAttemptsAfterSuccessfulLogin() {
        analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED"));
        analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED"));

        analyticsService.record(new UserActivityEvent("user-1", "LOGIN_SUCCESS"));

        assertFalse(analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED")));
        assertFalse(analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED")));
        assertTrue(analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED")));
    }

    @Test
    void shouldTrackFailedLoginAttemptsSeparatelyForEachUser() {
        analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED"));
        analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED"));

        assertFalse(analyticsService.record(new UserActivityEvent("user-2", "LOGIN_FAILED")));

        assertTrue(analyticsService.record(new UserActivityEvent("user-1", "LOGIN_FAILED")));
    }
}
