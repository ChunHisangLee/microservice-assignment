package com.jack.priceservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.dto.response.BTCPriceResponseDto;
import com.jack.priceservice.schedule.ScheduledTasks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PriceServiceImpl priceService;

    @BeforeEach
    void setUp() {
        // Manually set the btcPriceKey since @Value is not processed in unit tests
        ReflectionTestUtils.setField(priceService, "btcPriceKey", "BTCPrice:");

        // Mock that the RedisTemplate returns the ValueOperations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGetPrice_Success() throws Exception {
        // Arrange
        String btcPriceKey = "BTCPrice:";
        BTCPriceResponseDto dto = BTCPriceResponseDto.builder()
                .id(1L)
                .btcPrice(new BigDecimal("1234.56"))
                .build();
        String dtoJson = "{\"id\":1,\"btcPrice\":1234.56}";

        when(valueOperations.get(btcPriceKey)).thenReturn(dtoJson);
        when(objectMapper.readValue(dtoJson, BTCPriceResponseDto.class)).thenReturn(dto);

        // Act
        BigDecimal result = priceService.getPrice();

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("1234.56"), result);

        verify(valueOperations, times(1)).get(btcPriceKey);
        verify(objectMapper, times(1)).readValue(dtoJson, BTCPriceResponseDto.class);
    }

    @Test
    void testGetPrice_RedisReturnsNull() throws Exception {
        // Arrange
        String btcPriceKey = "BTCPrice:";

        when(valueOperations.get(btcPriceKey)).thenReturn(null);

        // Act
        BigDecimal result = priceService.getPrice();

        // Assert
        assertNull(result);

        verify(valueOperations, times(1)).get(btcPriceKey);
        verify(objectMapper, never()).readValue(anyString(), eq(BTCPriceResponseDto.class));
    }

    @Test
    void testGetPrice_InvalidJson() throws Exception {
        // Arrange
        String btcPriceKey = "BTCPrice:";
        String invalidJson = "invalid-json";

        when(valueOperations.get(btcPriceKey)).thenReturn(invalidJson);
        when(objectMapper.readValue(invalidJson, BTCPriceResponseDto.class)).thenThrow(new JsonProcessingException("Invalid JSON") {
        });

        // Act
        BigDecimal result = priceService.getPrice();

        // Assert
        assertNull(result);

        verify(valueOperations, times(1)).get(btcPriceKey);
        verify(objectMapper, times(1)).readValue(invalidJson, BTCPriceResponseDto.class);
    }

    @Test
    void testSetPriceWithId_Success() throws JsonProcessingException {
        // Arrange
        Long id = 42L;
        BigDecimal price = new BigDecimal("6543.21");
        String btcPriceKey = "BTCPrice:";  // Ensure this key matches the field in PriceServiceImpl

        BTCPriceResponseDto dto = BTCPriceResponseDto.builder()
                .id(id)
                .btcPrice(price)
                .build();

        String dtoJson = "{\"id\":42,\"btcPrice\":6543.21}";

        // Use any(BTCPriceResponseDto.class) to match the object more flexibly
        when(objectMapper.writeValueAsString(any(BTCPriceResponseDto.class))).thenReturn(dtoJson);

        // Act
        priceService.setPriceWithId(id, price);

        // Assert
        verify(objectMapper, times(1)).writeValueAsString(any(BTCPriceResponseDto.class));
        verify(valueOperations, times(1)).set(eq(btcPriceKey), eq(dtoJson), eq(Duration.ofMillis(ScheduledTasks.SCHEDULE_RATE_MS)));
    }
}
