package com.jack.priceservice.service.impl;

import com.jack.priceservice.schedule.ScheduledTasks;
import com.jack.priceservice.service.PriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
public class PriceServiceImpl implements PriceService {
    private static final Logger logger = LoggerFactory.getLogger(PriceServiceImpl.class);
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.redis.btc-price-key}")
    private String btcPriceKey;

    @Value("${initial.price:100.00}")
    private BigDecimal initialPrice;

    public PriceServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public BigDecimal getPrice() {
        logger.info("Fetching current BTC price from Redis with key: {}", btcPriceKey);
        // Fetch the price from Redis
        String priceStr = redisTemplate.opsForValue().get(btcPriceKey);
        BigDecimal price = null;

        if (priceStr != null) {
            try {
                price = new BigDecimal(priceStr);
                logger.info("Current BTC price retrieved from Redis: {}", price);
            } catch (NumberFormatException e) {
                logger.error("Failed to convert price from Redis to BigDecimal: {}", priceStr, e);
            }
        } else {
            logger.warn("BTC price not found in Redis. Falling back to initial price: {}", initialPrice);
        }

        return price != null ? price : initialPrice;
    }

    @Override
    public void setPrice(BigDecimal price) {
        logger.info("Setting current BTC price in Redis with key: {} to value: {}", btcPriceKey, price);
        redisTemplate.opsForValue().set(btcPriceKey, String.valueOf(price), Duration.ofMillis(ScheduledTasks.SCHEDULE_RATE_MS));
        logger.info("BTC price set successfully in Redis.");
    }
}
