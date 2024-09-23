package com.jack.transactionservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.TransactionType;
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
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionRedisServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransactionRedisServiceImpl transactionRedisService;

    private final String CACHE_PREFIX = "testPrefix:";
    private final long CACHE_TTL = 10L; // in minutes

    private TransactionDto sampleTransaction;

    @BeforeEach
    void setUp() {
        // Initialize a sample TransactionDto object
        sampleTransaction = TransactionDto.builder()
                .id(1L)
                .userId(100L)
                .btcPriceHistoryId(200L)
                .btcAmount(new BigDecimal("0.5"))
                .usdAmount(new BigDecimal("25000.00"))
                .transactionTime(LocalDateTime.now())
                .transactionType(TransactionType.BUY)
                .usdBalanceBefore(new BigDecimal("10000.00"))
                .btcBalanceBefore(new BigDecimal("1.0"))
                .usdBalanceAfter(new BigDecimal("7500.00"))
                .btcBalanceAfter(new BigDecimal("1.5"))
                .build();

        // Leniently mock the RedisTemplate to return the mocked ValueOperations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Set the cachePrefix and cacheTTL using ReflectionTestUtils
        ReflectionTestUtils.setField(transactionRedisService, "cachePrefix", CACHE_PREFIX);
        ReflectionTestUtils.setField(transactionRedisService, "cacheTTL", CACHE_TTL);

        // Replace the internally instantiated ObjectMapper with the mocked one
        ReflectionTestUtils.setField(transactionRedisService, "objectMapper", objectMapper);
    }

    @Test
    void saveTransactionToRedis_ShouldSaveTransactionSuccessfully() throws Exception {
        // Arrange
        String transactionJson = "{\"id\":1,\"userId\":100,\"btcPriceHistoryId\":200,\"btcAmount\":0.5," +
                "\"usdAmount\":25000.00,\"transactionTime\":\"2023-10-01T10:00:00\"," +
                "\"transactionType\":\"BUY\",\"usdBalanceBefore\":10000.00,\"btcBalanceBefore\":1.0," +
                "\"usdBalanceAfter\":7500.00,\"btcBalanceAfter\":1.5}";
        when(objectMapper.writeValueAsString(sampleTransaction)).thenReturn(transactionJson);

        // Act
        transactionRedisService.saveTransactionToRedis(sampleTransaction);

        // Assert
        String expectedKey = CACHE_PREFIX + sampleTransaction.getId();
        verify(valueOperations, times(1)).set(expectedKey, transactionJson, CACHE_TTL, TimeUnit.MINUTES);
    }

    @Test
    void saveTransactionToRedis_ShouldThrowException_WhenSerializationFails() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(sampleTransaction))
                .thenThrow(new JsonProcessingException("Serialization error") {
                });

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> transactionRedisService.saveTransactionToRedis(sampleTransaction), "Expected saveTransactionToRedis to throw, but it didn't");

        assertEquals("Failed to serialize TransactionDto to JSON for caching", exception.getMessage());

        // Verify that set was never called due to exception
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void getTransactionFromRedis_ShouldReturnTransaction_WhenTransactionExists() throws Exception {
        // Arrange
        String expectedKey = CACHE_PREFIX + sampleTransaction.getId();
        String transactionJson = "{\"id\":1,\"userId\":100,\"btcPriceHistoryId\":200,\"btcAmount\":0.5," +
                "\"usdAmount\":25000.00,\"transactionTime\":\"2023-10-01T10:00:00\"," +
                "\"transactionType\":\"BUY\",\"usdBalanceBefore\":10000.00,\"btcBalanceBefore\":1.0," +
                "\"usdBalanceAfter\":7500.00,\"btcBalanceAfter\":1.5}";
        when(valueOperations.get(expectedKey)).thenReturn(transactionJson);
        when(objectMapper.readValue(transactionJson, TransactionDto.class)).thenReturn(sampleTransaction);

        // Act
        TransactionDto retrievedTransaction = transactionRedisService.getTransactionFromRedis(sampleTransaction.getId());

        // Assert
        assertNotNull(retrievedTransaction, "Retrieved transaction should not be null");
        assertEquals(sampleTransaction.getId(), retrievedTransaction.getId(), "Transaction ID should match");
        assertEquals(sampleTransaction.getUserId(), retrievedTransaction.getUserId(), "User ID should match");
        assertEquals(sampleTransaction.getBtcPriceHistoryId(), retrievedTransaction.getBtcPriceHistoryId(), "BTC Price History ID should match");
        assertEquals(sampleTransaction.getBtcAmount(), retrievedTransaction.getBtcAmount(), "BTC Amount should match");
        assertEquals(sampleTransaction.getUsdAmount(), retrievedTransaction.getUsdAmount(), "USD Amount should match");
        assertEquals(sampleTransaction.getTransactionTime(), retrievedTransaction.getTransactionTime(), "Transaction Time should match");
        assertEquals(sampleTransaction.getTransactionType(), retrievedTransaction.getTransactionType(), "Transaction Type should match");
        assertEquals(sampleTransaction.getUsdBalanceBefore(), retrievedTransaction.getUsdBalanceBefore(), "USD Balance Before should match");
        assertEquals(sampleTransaction.getBtcBalanceBefore(), retrievedTransaction.getBtcBalanceBefore(), "BTC Balance Before should match");
        assertEquals(sampleTransaction.getUsdBalanceAfter(), retrievedTransaction.getUsdBalanceAfter(), "USD Balance After should match");
        assertEquals(sampleTransaction.getBtcBalanceAfter(), retrievedTransaction.getBtcBalanceAfter(), "BTC Balance After should match");

        verify(valueOperations, times(1)).get(expectedKey);
        verify(objectMapper, times(1)).readValue(transactionJson, TransactionDto.class);
    }

    @Test
    void getTransactionFromRedis_ShouldReturnNull_WhenTransactionDoesNotExist() throws JsonProcessingException {
        // Arrange
        String expectedKey = CACHE_PREFIX + sampleTransaction.getId();
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // Act
        TransactionDto retrievedTransaction = transactionRedisService.getTransactionFromRedis(sampleTransaction.getId());

        // Assert
        assertNull(retrievedTransaction, "Retrieved transaction should be null when not present in Redis");
        verify(valueOperations, times(1)).get(expectedKey);
        verify(objectMapper, never()).readValue(anyString(), eq(TransactionDto.class));
    }

    @Test
    void getTransactionFromRedis_ShouldThrowException_WhenDeserializationFails() throws Exception {
        // Arrange
        String expectedKey = CACHE_PREFIX + sampleTransaction.getId();
        String invalidJson = "{\"id\":1,\"userId\":100,\"btcPriceHistoryId\":200,\"btcAmount\":\"invalid_amount\"," +
                "\"usdAmount\":25000.00,\"transactionTime\":\"2023-10-01T10:00:00\"," +
                "\"transactionType\":\"BUY\",\"usdBalanceBefore\":10000.00,\"btcBalanceBefore\":1.0," +
                "\"usdBalanceAfter\":7500.00,\"btcBalanceAfter\":1.5}";
        when(valueOperations.get(expectedKey)).thenReturn(invalidJson);
        when(objectMapper.readValue(invalidJson, TransactionDto.class))
                .thenThrow(new JsonProcessingException("Deserialization error") {
                });

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                        transactionRedisService.getTransactionFromRedis(sampleTransaction.getId()),
                "Expected getTransactionFromRedis to throw, but it didn't");

        assertEquals("Failed to deserialize JSON to TransactionDto", exception.getMessage());

        verify(valueOperations, times(1)).get(expectedKey);
        verify(objectMapper, times(1)).readValue(invalidJson, TransactionDto.class);
    }

    @Test
    void deleteTransactionFromRedis_ShouldDeleteTransactionSuccessfully() {
        // Arrange
        String expectedKey = CACHE_PREFIX + sampleTransaction.getId();
        when(redisTemplate.delete(expectedKey)).thenReturn(true);

        // Act
        transactionRedisService.deleteTransactionFromRedis(sampleTransaction.getId());

        // Assert
        verify(redisTemplate, times(1)).delete(expectedKey);
    }

    @Test
    void deleteTransactionFromRedis_ShouldHandleNonExistingTransaction() {
        // Arrange
        String expectedKey = CACHE_PREFIX + sampleTransaction.getId();
        when(redisTemplate.delete(expectedKey)).thenReturn(false); // Indicates nothing was deleted

        // Act
        transactionRedisService.deleteTransactionFromRedis(sampleTransaction.getId());

        // Assert
        verify(redisTemplate, times(1)).delete(expectedKey);
        // Optionally, you can verify logging or other behaviors if necessary
    }

    @Test
    void saveTransactionToRedis_ShouldHandleNullTransactionDto() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> transactionRedisService.saveTransactionToRedis(null),
                "Expected saveTransactionToRedis to throw NullPointerException when transactionDto is null");

        // The exception occurs at transactionDto.getId(), so message may vary
        // Optionally, verify no interaction with RedisTemplate
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void getTransactionFromRedis_ShouldHandleNullTransactionId() throws JsonProcessingException {
        // Act & Assert
        String expectedKey = CACHE_PREFIX + null;
        when(valueOperations.get(expectedKey)).thenReturn(null);

        TransactionDto retrievedTransaction = transactionRedisService.getTransactionFromRedis(null);

        assertNull(retrievedTransaction, "Retrieved transaction should be null when transactionId is null");
        verify(valueOperations, times(1)).get(expectedKey);
        verify(objectMapper, never()).readValue(anyString(), eq(TransactionDto.class));
    }

    @Test
    void deleteTransactionFromRedis_ShouldHandleNullTransactionId() {
        // Arrange
        String expectedKey = CACHE_PREFIX + null;
        when(redisTemplate.delete(expectedKey)).thenReturn(false);

        // Act
        transactionRedisService.deleteTransactionFromRedis(null);

        // Assert
        verify(redisTemplate, times(1)).delete(expectedKey);
    }
}
