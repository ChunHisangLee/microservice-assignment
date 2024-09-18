package com.jack.outboxservice.repository;

import com.jack.common.constants.EventStatus;
import com.jack.outboxservice.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findByStatus(EventStatus status);
}
