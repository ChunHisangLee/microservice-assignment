package com.jack.priceservice.schedule;

import com.jack.priceservice.entity.BTCPriceHistory;
import com.jack.priceservice.repository.BTCPriceHistoryRepository;
import com.jack.priceservice.service.PriceService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
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

    @Getter
    private boolean isIncreasing = true;

    @Getter
    private double currentPrice = MIN_PRICE;

    private final PriceService priceService;
    private final BTCPriceHistoryRepository btcPriceHistoryRepository;

    public ScheduledTasks(PriceService priceService, BTCPriceHistoryRepository btcPriceHistoryRepository) {
        this.priceService = priceService;
        this.btcPriceHistoryRepository = btcPriceHistoryRepository;
    }

    @PostConstruct
    protected void saveInitialPrice() {
        priceService.setPrice(BigDecimal.valueOf(currentPrice));

        BTCPriceHistory initialPriceHistory = new BTCPriceHistory();
        initialPriceHistory.setPrice(BigDecimal.valueOf(currentPrice));
        initialPriceHistory.setTimestamp(LocalDateTime.now());
        btcPriceHistoryRepository.save(initialPriceHistory);

        log.info("Saved initial BTC Price to Redis and database: {}", currentPrice);
    }

    @Scheduled(fixedRate = SCHEDULE_RATE_MS)
    public void updateCurrentPrice() {
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

        // Save the updated price to Redis
        priceService.setPrice(BigDecimal.valueOf(currentPrice));

        // Save the updated price to the database
        BTCPriceHistory priceHistory = new BTCPriceHistory();
        priceHistory.setPrice(BigDecimal.valueOf(currentPrice));
        priceHistory.setTimestamp(LocalDateTime.now());
        btcPriceHistoryRepository.save(priceHistory);

        log.info("Saved updated BTC Price to Redis and database: {}", currentPrice);
    }
}
