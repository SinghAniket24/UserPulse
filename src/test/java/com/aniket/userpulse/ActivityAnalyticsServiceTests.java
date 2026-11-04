package com.aniket.userpulse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ActivityAnalyticsServiceTests {

    private ActivityEventRepository repository;
    private ActivityAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        repository = mock(ActivityEventRepository.class);
        analyticsService = new ActivityAnalyticsService(repository);
        when(repository.insertIfNotExists(any(), any(), any())).thenReturn(1);
    }

    @Test
    void shouldPersistNewEventSuccessfully() {
        UserActivityEvent event = new UserActivityEvent("evt-1", "user-1", "LOGIN_SUCCESS");
        when(repository.insertIfNotExists("evt-1", "user-1", "LOGIN_SUCCESS")).thenReturn(1);

        boolean alert = analyticsService.record(event);

        assertFalse(alert);
        verify(repository, times(1)).insertIfNotExists("evt-1", "user-1", "LOGIN_SUCCESS");
    }

    @Test
    void shouldIgnoreRepeatedEventsWithSameEventId() {
        UserActivityEvent firstEvent = new UserActivityEvent("evt-1", "user-1", "LOGIN_SUCCESS");
        UserActivityEvent duplicateEvent = new UserActivityEvent("evt-1", "user-1", "LOGIN_SUCCESS");

        when(repository.insertIfNotExists("evt-1", "user-1", "LOGIN_SUCCESS"))
                .thenReturn(1)
                .thenReturn(0);

        boolean firstResult = analyticsService.record(firstEvent);
        boolean duplicateResult = analyticsService.record(duplicateEvent);

        assertFalse(firstResult);
        assertFalse(duplicateResult);
        verify(repository, times(2)).insertIfNotExists("evt-1", "user-1", "LOGIN_SUCCESS");
    }

    @Test
    void shouldProcessDifferentEventIdsForSameUserIndependently() {
        UserActivityEvent event1 = new UserActivityEvent("evt-1", "user-1", "LOGIN_SUCCESS");
        UserActivityEvent event2 = new UserActivityEvent("evt-2", "user-1", "LOGIN_SUCCESS");

        when(repository.insertIfNotExists("evt-1", "user-1", "LOGIN_SUCCESS")).thenReturn(1);
        when(repository.insertIfNotExists("evt-2", "user-1", "LOGIN_SUCCESS")).thenReturn(1);

        boolean result1 = analyticsService.record(event1);
        boolean result2 = analyticsService.record(event2);

        assertFalse(result1);
        assertFalse(result2);
        verify(repository, times(1)).insertIfNotExists("evt-1", "user-1", "LOGIN_SUCCESS");
        verify(repository, times(1)).insertIfNotExists("evt-2", "user-1", "LOGIN_SUCCESS");
    }

    @Test
    void duplicateLoginFailedEventsShouldNotIncrementFailedLoginCounter() {
        UserActivityEvent failEvent = new UserActivityEvent("evt-dup", "user-1", "LOGIN_FAILED");

        when(repository.insertIfNotExists("evt-dup", "user-1", "LOGIN_FAILED"))
                .thenReturn(1)   // first delivery -> new
                .thenReturn(0)   // second delivery -> duplicate
                .thenReturn(0);  // third delivery -> duplicate

        assertFalse(analyticsService.record(failEvent));
        assertFalse(analyticsService.record(failEvent));
        assertFalse(analyticsService.record(failEvent));

        verify(repository, times(3)).insertIfNotExists("evt-dup", "user-1", "LOGIN_FAILED");
    }

    @Test
    void duplicateEventsCannotTriggerFalseSecurityAlert() {
        UserActivityEvent fail1 = new UserActivityEvent("evt-fail-1", "user-1", "LOGIN_FAILED");
        UserActivityEvent fail2 = new UserActivityEvent("evt-fail-2", "user-1", "LOGIN_FAILED");

        when(repository.insertIfNotExists("evt-fail-1", "user-1", "LOGIN_FAILED"))
                .thenReturn(1)   // first time -> new
                .thenReturn(0);  // duplicate redelivery -> ignored

        when(repository.insertIfNotExists("evt-fail-2", "user-1", "LOGIN_FAILED"))
                .thenReturn(1);  // first time -> new

        // 1st failed attempt
        assertFalse(analyticsService.record(fail1));
        // Duplicate delivery of 1st failed attempt -> ignored, counter stays at 1
        assertFalse(analyticsService.record(fail1));
        // 2nd failed attempt (distinct event) -> counter becomes 2
        assertFalse(analyticsService.record(fail2));
        // Another duplicate delivery of 1st failed attempt -> ignored, counter stays at 2
        assertFalse(analyticsService.record(fail1));

        // Threshold is 3; duplicates were ignored so counter never reached 3
    }

    @Test
    void threeDistinctConsecutiveFailedLoginsShouldTriggerAlert() {
        UserActivityEvent fail1 = new UserActivityEvent("evt-1", "user-1", "LOGIN_FAILED");
        UserActivityEvent fail2 = new UserActivityEvent("evt-2", "user-1", "LOGIN_FAILED");
        UserActivityEvent fail3 = new UserActivityEvent("evt-3", "user-1", "LOGIN_FAILED");
        UserActivityEvent fail4 = new UserActivityEvent("evt-4", "user-1", "LOGIN_FAILED");

        assertFalse(analyticsService.record(fail1));
        assertFalse(analyticsService.record(fail2));
        assertTrue(analyticsService.record(fail3));
        assertFalse(analyticsService.record(fail4));
    }

    @Test
    void shouldResetFailedAttemptsAfterSuccessfulLogin() {
        UserActivityEvent fail1 = new UserActivityEvent("evt-fail-1", "user-1", "LOGIN_FAILED");
        UserActivityEvent fail2 = new UserActivityEvent("evt-fail-2", "user-1", "LOGIN_FAILED");
        UserActivityEvent success = new UserActivityEvent("evt-success", "user-1", "LOGIN_SUCCESS");
        UserActivityEvent fail3 = new UserActivityEvent("evt-fail-3", "user-1", "LOGIN_FAILED");
        UserActivityEvent fail4 = new UserActivityEvent("evt-fail-4", "user-1", "LOGIN_FAILED");
        UserActivityEvent fail5 = new UserActivityEvent("evt-fail-5", "user-1", "LOGIN_FAILED");

        analyticsService.record(fail1);
        analyticsService.record(fail2);

        analyticsService.record(success);

        assertFalse(analyticsService.record(fail3));
        assertFalse(analyticsService.record(fail4));
        assertTrue(analyticsService.record(fail5));
    }

    @Test
    void analyticsCountsRemainCorrectWhenDuplicatesAreReceived() {
        UserActivityEvent firstEvent = new UserActivityEvent("evt-1", "user-1", "LOGIN_SUCCESS");
        UserActivityEvent duplicateEvent = new UserActivityEvent("evt-1", "user-1", "LOGIN_SUCCESS");

        when(repository.insertIfNotExists("evt-1", "user-1", "LOGIN_SUCCESS"))
                .thenReturn(1)
                .thenReturn(0);

        when(repository.findEventTypeCounts()).thenReturn(Collections.singletonList(
                new Object[]{"LOGIN_SUCCESS", 1L}
        ));

        analyticsService.record(firstEvent);
        analyticsService.record(duplicateEvent);

        Map<String, Long> counts = analyticsService.getCounts();
        assertEquals(1L, counts.get("LOGIN_SUCCESS"));
    }

    @Test
    void shouldCountRepeatedEvents() {
        when(repository.findEventTypeCounts()).thenReturn(Collections.singletonList(
                new Object[]{"LOGIN_SUCCESS", 2L}
        ));

        assertEquals(2L, analyticsService.getCounts().get("LOGIN_SUCCESS"));
    }

    @Test
    void shouldTrackDifferentEventTypesSeparately() {
        when(repository.findEventTypeCounts()).thenReturn(List.of(
                new Object[]{"LOGIN_SUCCESS", 1L},
                new Object[]{"USER_SIGNED_UP", 1L}
        ));

        assertEquals(
                Map.of("LOGIN_SUCCESS", 1L, "USER_SIGNED_UP", 1L),
                analyticsService.getCounts());
    }

    @Test
    void shouldReturnEmptyCountsInitially() {
        when(repository.findEventTypeCounts()).thenReturn(Collections.emptyList());

        assertTrue(analyticsService.getCounts().isEmpty());
    }

    @Test
    void shouldTrackFailedLoginAttemptsSeparatelyForEachUser() {
        analyticsService.record(new UserActivityEvent("evt-u1-1", "user-1", "LOGIN_FAILED"));
        analyticsService.record(new UserActivityEvent("evt-u1-2", "user-1", "LOGIN_FAILED"));

        assertFalse(analyticsService.record(new UserActivityEvent("evt-u2-1", "user-2", "LOGIN_FAILED")));

        assertTrue(analyticsService.record(new UserActivityEvent("evt-u1-3", "user-1", "LOGIN_FAILED")));
    }

    @Test
    void shouldThrowExceptionWhenEventIsNull() {
        assertThrows(IllegalArgumentException.class, () -> analyticsService.record(null));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldThrowExceptionWhenEventTypeIsNull() {
        assertThrows(IllegalArgumentException.class, () -> analyticsService.record(new UserActivityEvent("evt-1", "user-1", null)));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldThrowExceptionWhenEventTypeIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> analyticsService.record(new UserActivityEvent("evt-1", "user-1", "   ")));
        verifyNoInteractions(repository);
    }
}
