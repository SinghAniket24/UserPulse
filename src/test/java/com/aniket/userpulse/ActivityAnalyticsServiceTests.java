package com.aniket.userpulse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void shouldThrowExceptionWhenEventIsNull() {
        assertThrows(IllegalArgumentException.class, () -> analyticsService.record(null));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldThrowExceptionWhenEventTypeIsNull() {
        assertThrows(IllegalArgumentException.class, () -> analyticsService.record(new UserActivityEvent("user-1", null)));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldThrowExceptionWhenEventTypeIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> analyticsService.record(new UserActivityEvent("user-1", "   ")));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldSaveValidEventToRepositoryExactlyOnce() {
        UserActivityEvent event = new UserActivityEvent("user-1", "LOGIN_SUCCESS");

        analyticsService.record(event);

        ArgumentCaptor<ActivityEventEntity> captor = ArgumentCaptor.forClass(ActivityEventEntity.class);
        verify(repository, times(1)).save(captor.capture());

        ActivityEventEntity savedEntity = captor.getValue();
        assertEquals("user-1", savedEntity.getUserId());
        assertEquals("LOGIN_SUCCESS", savedEntity.getEventType());
    }
}
