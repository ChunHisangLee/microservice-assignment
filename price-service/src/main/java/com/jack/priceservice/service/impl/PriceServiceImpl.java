package com.jack.priceservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.dto.response.BTCPriceResponseDto;
import com.jack.priceservice.config.PriceServiceProperties;
import com.jack.priceservice.schedule.ScheduledTasks;
import com.jack.priceservice.service.PriceService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class PriceServiceImpl implements PriceService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PriceServiceProperties properties;

    @Override
    public BigDecimal getPrice() {
        String btcPriceKey = properties.getBtcPriceKey();
        log.info("Fetching current BTC price from Redis with key: {}", btcPriceKey);
        // Fetch the price from Redis
        return getPriceFromRedis(btcPriceKey)
                .map(BTCPriceResponseDto::getBtcPrice)
                .orElseGet(() -> {
                    log.warn("BTC price data not found in Redis for key: {}", btcPriceKey);
                    return BigDecimal.ZERO;  // Return a default value or throw a custom exception
                });
    }

    @Override
    public void setPriceWithId(Long id,@NonNull BigDecimal price) {
        BTCPriceResponseDto dto = BTCPriceResponseDto.builder()
                .id(id)
                .btcPrice(price)
                .build();
        setPriceInRedis(dto);
    }

    private Optional<BTCPriceResponseDto> getPriceFromRedis(String key) {
        try {
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            String json = valueOperations.get(key);

            if (json != null) {
                return Optional.of(objectMapper.readValue(json, BTCPriceResponseDto.class));
            }
        } catch (Exception e) {
            log.error("Error retrieving BTC price from Redis for key: {}", key, e);
        }

        return Optional.empty();
    }

    private void setPriceInRedis(BTCPriceResponseDto dto) {
        try {
            String priceJson = objectMapper.writeValueAsString(dto);
            String btcPriceKey = properties.getBtcPriceKey();
            redisTemplate.opsForValue().set(btcPriceKey, priceJson, Duration.ofMillis(ScheduledTasks.SCHEDULE_RATE_MS));
            log.info("Set BTC price in Redis with key {} and data: {}", btcPriceKey, priceJson);
        } catch (JsonProcessingException e) {
            log.error("Error serializing BTC price data for ID: {}", dto.getId(), e);
        }
    }
}
