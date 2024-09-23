package com.jack.priceservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.dto.response.BTCPriceResponseDto;
import com.jack.priceservice.schedule.ScheduledTasks;
import com.jack.priceservice.service.PriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
public class PriceServiceImpl implements PriceService {
    private static final Logger logger = LoggerFactory.getLogger(PriceServiceImpl.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.btc-price-key:#{T(com.jack.common.constants.ApplicationConstants).BTC_PRICE_KEY}}")
    private String btcPriceKey;

    public PriceServiceImpl(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public BigDecimal getPrice() {
        logger.info("Fetching current BTC price from Redis with key: {}", btcPriceKey);
        // Fetch the price from Redis
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String json = valueOperations.get(btcPriceKey);

        if (json == null) {
            logger.warn("No data found in Redis for key: {}", btcPriceKey);
            return null;
        }

        try {
            // Convert the JSON string back to a DTO
            BTCPriceResponseDto priceResponseDto = objectMapper.readValue(json, BTCPriceResponseDto.class);
            return priceResponseDto.getBtcPrice();
        } catch (Exception e) {
            logger.error("Error deserializing BTCPriceResponseDto from JSON", e);
            return null;
        }
    }

    @Override
    public void setPriceWithId(Long id, BigDecimal price) {
        if (price == null) {
            logger.warn("Attempted to set null price in Redis.");
            return;
        }

        try {
            BTCPriceResponseDto dto = BTCPriceResponseDto.builder()
                    .id(id)
                    .btcPrice(price)
                    .build();

            String priceJson = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(btcPriceKey, priceJson, Duration.ofMillis(ScheduledTasks.SCHEDULE_RATE_MS));
            logger.info("Set price with ID in Redis: {}", priceJson);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing BTC price data", e);
        }
    }
}
