package com.jack.outboxservice.repository;

import com.jack.userservice.entity.OutboxEvent;
import com.jack.userservice.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatus(EventStatus status); // Fetch pending events
}
