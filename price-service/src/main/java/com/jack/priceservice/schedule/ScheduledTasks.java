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
import java.time.LocalDateTime;

@Component
@Slf4j
public class ScheduledTasks {

    private static final double MIN_PRICE = 100;
    private static final double MAX_PRICE = 460;
    private static final double PRICE_INCREMENT = 10;
    public static final int SCHEDULE_RATE_MS = 5 * 1000;

    private boolean isIncreasing = true;
    private double currentPrice = ApplicationConstants.INITIAL_PRICE;

    private final PriceService priceService;
    private final BTCPriceHistoryRepository btcPriceHistoryRepository;

    public ScheduledTasks(PriceService priceService, BTCPriceHistoryRepository btcPriceHistoryRepository) {
        this.priceService = priceService;
        this.btcPriceHistoryRepository = btcPriceHistoryRepository;
    }

    @PostConstruct
    protected void saveInitialPrice() {
        // Create and save the initial BTCPriceHistory entity
        BTCPriceHistory initialPriceHistory = new BTCPriceHistory();
        initialPriceHistory.setPrice(BigDecimal.valueOf(currentPrice));
        initialPriceHistory.setTimestamp(LocalDateTime.now());
        BTCPriceHistory savedInitialPrice = btcPriceHistoryRepository.save(initialPriceHistory);

        // Save the price to Redis with the btchistoryID
        priceService.setPriceWithId(savedInitialPrice.getId(), BigDecimal.valueOf(currentPrice));

        log.info("Saved initial BTC Price to Redis and database: ID={}, Price={}",
                savedInitialPrice.getId(), currentPrice);
    }

    @Scheduled(fixedRate = SCHEDULE_RATE_MS)
    public void updateCurrentPrice() {
        // Update the current price based on the increasing flag
        if (isIncreasing) {
            currentPrice += PRICE_INCREMENT;
            if (currentPrice >= MAX_PRICE) {
                isIncreasing = false;
            }
        } else {
            currentPrice -= PRICE_INCREMENT;
            if (currentPrice <= MIN_PRICE) {
                isIncreasing = true;
            }
        }

        log.info("Updated BTC Price: {}", currentPrice);

        // Create and save the new BTCPriceHistory entity
        BTCPriceHistory priceHistory = new BTCPriceHistory();
        priceHistory.setPrice(BigDecimal.valueOf(currentPrice));
        priceHistory.setTimestamp(LocalDateTime.now());
        BTCPriceHistory savedPriceHistory = btcPriceHistoryRepository.save(priceHistory);

        // Save the updated price to Redis with the btchistoryID
        priceService.setPriceWithId(savedPriceHistory.getId(), BigDecimal.valueOf(currentPrice));

        log.info("Saved updated BTC Price to Redis and database: ID={}, Price={}",
                savedPriceHistory.getId(), currentPrice);
    }
}
