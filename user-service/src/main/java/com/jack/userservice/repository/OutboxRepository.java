package com.jack.userservice.repository;

import com.jack.userservice.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findTop100ByProcessedFalseOrderByCreatedAtAsc();

    @Query("SELECT MAX(o.sequenceNumber) FROM Outbox o WHERE o.aggregateType = :aggregateType AND o.aggregateId = :aggregateId")
    Optional<Long> findMaxSequenceNumber(@Param("aggregateType") String aggregateType, @Param("aggregateId") String aggregateId);
}
