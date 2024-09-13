package com.jack.priceservice.repository;

import com.jack.priceservice.entity.BTCPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BTCPriceHistoryRepository extends JpaRepository<BTCPriceHistory, Long> {
    Optional<BTCPriceHistory> findTopByOrderByTimestampDesc();
}
