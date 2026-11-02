package com.aniket.userpulse;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ActivityEventRepository extends JpaRepository<ActivityEventEntity, Long> {

    @Query("SELECT e.eventType, COUNT(e) FROM ActivityEventEntity e GROUP BY e.eventType")
    List<Object[]> findEventTypeCounts();
}

