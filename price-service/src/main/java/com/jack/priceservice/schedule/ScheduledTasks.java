package com.jack.priceservice.schedule;

import com.jack.common.constants.ApplicationConstants;
import com.jack.priceservice.entity.BTCPriceHistory;
import com.jack.priceservice.repository.BTCPriceHistoryRepository;
import com.jack.priceservice.service.PriceService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class ScheduledTasks {
    private static final BigDecimal MIN_PRICE = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(460);
    private static final BigDecimal PRICE_INCREMENT = BigDecimal.valueOf(10);
    public static final int SCHEDULE_RATE_MS = 5 * 1000;

    private boolean isIncreasing = true;
    private BigDecimal currentPrice = ApplicationConstants.INITIAL_PRICE;

    private final PriceService priceService;
    private final BTCPriceHistoryRepository btcPriceHistoryRepository;

    public ScheduledTasks(PriceService priceService, BTCPriceHistoryRepository btcPriceHistoryRepository) {
        this.priceService = priceService;
        this.btcPriceHistoryRepository = btcPriceHistoryRepository;
    }

    @PostConstruct
    protected void saveInitialPrice() {
        // Create and save the initial BTCPriceHistory entity
        BTCPriceHistory initialPriceHistory = BTCPriceHistory.builder()
                .price(currentPrice)
                .build();
        BTCPriceHistory savedInitialPrice = btcPriceHistoryRepository.save(initialPriceHistory);

        // Save the price to Redis with the btchistoryID
        priceService.setPriceWithId(savedInitialPrice.getId(), currentPrice);

        log.info("Saved initial BTC Price to Redis and database: ID={}, Price={}", savedInitialPrice.getId(), currentPrice);
    }

    @Scheduled(fixedRate = SCHEDULE_RATE_MS)
    public void updateCurrentPrice() {
        // Update the current price based on the increasing flag
        if (isIncreasing) {
            currentPrice = currentPrice.add(PRICE_INCREMENT);

            if (currentPrice.compareTo(MAX_PRICE) >= 0) {
                isIncreasing = false;
            }
        } else {
            currentPrice = currentPrice.subtract(PRICE_INCREMENT);

            if (currentPrice.compareTo(MIN_PRICE) <= 0) {
                isIncreasing = true;
            }
        }

        log.info("Updated BTC Price: {}", currentPrice);

        // Create and save the new BTCPriceHistory entity
        BTCPriceHistory priceHistory = BTCPriceHistory.builder()
                .price(currentPrice)
                .build();
        BTCPriceHistory savedPriceHistory = btcPriceHistoryRepository.save(priceHistory);

        // Save the updated price to Redis with the btchistoryID
        priceService.setPriceWithId(savedPriceHistory.getId(), currentPrice);

        log.info("Saved updated BTC Price to Redis and database: ID={}, Price={}", savedPriceHistory.getId(), currentPrice);
    }
}
