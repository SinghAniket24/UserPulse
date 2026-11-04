package com.aniket.userpulse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"
})
class ActivityEventRepositoryTests {

    @Autowired
    private ActivityEventRepository repository;

    private final List<String> testEventIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String eventId : testEventIds) {
            repository.findByEventId(eventId).ifPresent(repository::delete);
        }
        testEventIds.clear();
    }

    @Test
    void shouldInsertNewEventAndReturnOne() {
        long baselineCount = repository.count();
        String eventId = "test-" + UUID.randomUUID();
        testEventIds.add(eventId);

        int rowsAffected = repository.insertIfNotExists(eventId, "user-test", "LOGIN_SUCCESS");

        assertEquals(1, rowsAffected);
        assertEquals(baselineCount + 1, repository.count());

        Optional<ActivityEventEntity> found = repository.findByEventId(eventId);
        assertTrue(found.isPresent());
        assertEquals(eventId, found.get().getEventId());
        assertEquals("user-test", found.get().getUserId());
        assertEquals("LOGIN_SUCCESS", found.get().getEventType());
    }

    @Test
    void shouldIgnoreDuplicateEventIdAndReturnZero() {
        long baselineCount = repository.count();
        String eventId = "test-" + UUID.randomUUID();
        testEventIds.add(eventId);

        int firstInsert = repository.insertIfNotExists(eventId, "user-test", "LOGIN_SUCCESS");
        assertEquals(1, firstInsert);
        assertEquals(baselineCount + 1, repository.count());

        int duplicateInsert = repository.insertIfNotExists(eventId, "user-test", "LOGIN_SUCCESS");
        assertEquals(0, duplicateInsert);
        assertEquals(baselineCount + 1, repository.count());
    }

    @Test
    void shouldInsertTwoEventsWithDifferentEventIds() {
        long baselineCount = repository.count();
        String eventId1 = "test-" + UUID.randomUUID();
        String eventId2 = "test-" + UUID.randomUUID();
        testEventIds.add(eventId1);
        testEventIds.add(eventId2);

        int firstInsert = repository.insertIfNotExists(eventId1, "user-1", "LOGIN_SUCCESS");
        int secondInsert = repository.insertIfNotExists(eventId2, "user-2", "LOGIN_FAILED");

        assertEquals(1, firstInsert);
        assertEquals(1, secondInsert);
        assertEquals(baselineCount + 2, repository.count());

        assertTrue(repository.findByEventId(eventId1).isPresent());
        assertTrue(repository.findByEventId(eventId2).isPresent());
    }

    @Test
    void shouldContainOnlyOneRowForRepeatedEventId() {
        String eventId = "test-" + UUID.randomUUID();
        testEventIds.add(eventId);

        int firstInsert = repository.insertIfNotExists(eventId, "user-1", "LOGIN_SUCCESS");
        int secondInsert = repository.insertIfNotExists(eventId, "user-1-different", "USER_SIGNED_UP");

        assertEquals(1, firstInsert);
        assertEquals(0, secondInsert);

        Optional<ActivityEventEntity> found = repository.findByEventId(eventId);
        assertTrue(found.isPresent());
        assertEquals("user-1", found.get().getUserId());
        assertEquals("LOGIN_SUCCESS", found.get().getEventType());
    }

    @Test
    void existingRecordsRemainUnaffected() {
        long baselineCount = repository.count();
        List<ActivityEventEntity> allInitialRecords = repository.findAll();

        String eventId = "test-" + UUID.randomUUID();
        testEventIds.add(eventId);

        int rows = repository.insertIfNotExists(eventId, "user-temp", "VIDEO_PLAYED");
        assertEquals(1, rows);

        // Verify all initial records still exist with the exact same values
        for (ActivityEventEntity initial : allInitialRecords) {
            Optional<ActivityEventEntity> current = repository.findById(initial.getId());
            assertTrue(current.isPresent());
            assertEquals(initial.getEventId(), current.get().getEventId());
            assertEquals(initial.getUserId(), current.get().getUserId());
            assertEquals(initial.getEventType(), current.get().getEventType());
        }

        // Verify count is exactly baseline + 1
        assertEquals(baselineCount + 1, repository.count());
    }
}
