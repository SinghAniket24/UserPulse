package com.aniket.userpulse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserActivityConsumerTests {

    private ActivityAnalyticsService analyticsService;
    private UserActivityConsumer consumer;

    @BeforeEach
    void setUp() {
        analyticsService = mock(ActivityAnalyticsService.class);
        consumer = new UserActivityConsumer(analyticsService);
    }

    @Test
    void shouldPassEventWithEventIdUnchangedToAnalyticsService() {
        UserActivityEvent event = new UserActivityEvent("evt-123", "user-1", "LOGIN_SUCCESS");
        when(analyticsService.record(event)).thenReturn(false);

        consumer.consume(event);

        verify(analyticsService).record(event);
    }

    @Test
    void shouldCompleteNormallyWhenRecordReturnsFalseForDuplicate() {
        UserActivityEvent duplicateEvent = new UserActivityEvent("evt-dup", "user-1", "LOGIN_SUCCESS");
        when(analyticsService.record(duplicateEvent)).thenReturn(false);

        assertDoesNotThrow(() -> consumer.consume(duplicateEvent));
        verify(analyticsService).record(duplicateEvent);
    }

    @Test
    void shouldTriggerSecurityAlertWhenRecordReturnsTrue() {
        UserActivityEvent failEvent = new UserActivityEvent("evt-fail-3", "user-1", "LOGIN_FAILED");
        when(analyticsService.record(failEvent)).thenReturn(true);

        assertDoesNotThrow(() -> consumer.consume(failEvent));
        verify(analyticsService).record(failEvent);
    }

    @Test
    void shouldHandleNullEventGracefully() {
        assertDoesNotThrow(() -> consumer.consume(null));
        verifyNoInteractions(analyticsService);
    }
}
