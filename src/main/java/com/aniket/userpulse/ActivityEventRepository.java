package com.aniket.userpulse;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ActivityEventRepository extends JpaRepository<ActivityEventEntity, Long> {

    @Query("SELECT e.eventType, COUNT(e) FROM ActivityEventEntity e GROUP BY e.eventType")
    List<Object[]> findEventTypeCounts();

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO activity_events (event_id, user_id, event_type) " +
                   "VALUES (:eventId, :userId, :eventType) " +
                   "ON CONFLICT (event_id) DO NOTHING",
           nativeQuery = true)
    int insertIfNotExists(@Param("eventId") String eventId,
                          @Param("userId") String userId,
                          @Param("eventType") String eventType);

    Optional<ActivityEventEntity> findByEventId(String eventId);

    @Query(value = "SELECT COUNT(*) " +
                   "FROM activity_events " +
                   "WHERE user_id = :userId " +
                   "  AND event_type = 'LOGIN_FAILED' " +
                   "  AND id > COALESCE( " +
                   "      (SELECT MAX(id) " +
                   "       FROM activity_events " +
                   "       WHERE user_id = :userId " +
                   "         AND event_type = 'LOGIN_SUCCESS'), " +
                   "      0 " +
                   "  )",
           nativeQuery = true)
    int countConsecutiveFailedLogins(@Param("userId") String userId);
}
