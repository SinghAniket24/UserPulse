package com.aniket.userpulse;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityEventRepository extends JpaRepository<ActivityEventEntity, Long> {
}
