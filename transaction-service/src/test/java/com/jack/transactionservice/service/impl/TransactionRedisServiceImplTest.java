package com.jack.transactionservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.dto.response.BTCPriceResponseDto;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for TransactionRedisServiceImpl.
 * This class tests the interaction with Redis for saving and retrieving transactions.
 */
@ExtendWith(MockitoExtension.class)
class TransactionRedisServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Spy
    private ObjectMapper objectMapper2 = new ObjectMapper();

    @InjectMocks
    private TransactionRedisServiceImpl transactionRedisService;

    private final String CACHE_PREFIX = "testPrefix:";
    private final long CACHE_TTL = 10L; // in minutes

    private TransactionDto sampleTransaction;

    /**
     * Sets up the test environment before each test.
     * Initializes a sample TransactionDto and configures the mocks.
     */
    @BeforeEach
    void setUp() {
        // Initialize a sample TransactionDto object using Builder pattern
        sampleTransaction = TransactionDto.builder()
                .id(1L)
                .userId(100L)
                .btcPriceHistoryId(200L)
                .btcAmount(BigDecimal.valueOf(0.5))
                .usdAmount(BigDecimal.valueOf(25000.00))
                .transactionTime(LocalDateTime.of(2023, 10, 1, 10, 0))
                .transactionType(TransactionType.BUY)
                .usdBalanceBefore(BigDecimal.valueOf(10000.00))
                .btcBalanceBefore(BigDecimal.valueOf(1.0))
                .usdBalanceAfter(BigDecimal.valueOf(7500.00))
                .btcBalanceAfter(BigDecimal.valueOf(1.5))
                .build();

        // Mock the RedisTemplate to return the mocked ValueOperations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Set the cachePrefix and cacheTTL using ReflectionTestUtils
        ReflectionTestUtils.setField(transactionRedisService, "cachePrefix", CACHE_PREFIX);
        ReflectionTestUtils.setField(transactionRedisService, "cacheTTL", CACHE_TTL);

        // Replace the internally instantiated ObjectMapper with the mocked one
        ReflectionTestUtils.setField(transactionRedisService, "objectMapper", objectMapper);
    }

    /**
     * Tests that a transaction is saved to Redis successfully.
     * Verifies that the correct key and value are set with the appropriate TTL.
     */
    @Test
    void saveTransactionToRedis_ShouldSaveTransactionSuccessfully() throws Exception {
        // Arrange
        String transactionJson = "{\"id\":1,\"userId\":100,\"btcPriceHistoryId\":200,\"btcAmount\":0.5," +
                "\"usdAmount\":25000.00,\"transactionTime\":\"2023-10-01T10:00:00\"," +
                "\"transactionType\":\"BUY\",\"usdBalanceBefore\":10000.00,\"btcBalanceBefore\":1.0," +
                "\"usdBalanceAfter\":7500.00,\"btcBalanceAfter\":1.5}";

        // Stub ObjectMapper to return the JSON string when serialization is called
        when(objectMapper.writeValueAsString(sampleTransaction)).thenReturn(transactionJson);

        // Act
        transactionRedisService.saveTransactionToRedis(sampleTransaction);

        // Assert
        String expectedKey = CACHE_PREFIX + sampleTransaction.getId();
        verify(valueOperations, times(1)).set(expectedKey, transactionJson, CACHE_TTL, TimeUnit.MINUTES);
    }

    /**
     * Tests that an exception is thrown when serialization fails during saving.
     * Verifies that the Redis set operation is never called.
     */
    @Test
    void saveTransactionToRedis_ShouldThrowException_WhenSerializationFails() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(sampleTransaction))
                .thenThrow(new JsonProcessingException("Serialization error") {
                });

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                        transactionRedisService.saveTransactionToRedis(sampleTransaction),
                "Expected saveTransactionToRedis to throw, but it didn't");

        assertEquals("Failed to serialize TransactionDto to JSON for caching", exception.getMessage());

        // Verify that set was never called due to exception
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    /**
     * Tests that a transaction is retrieved from Redis successfully when it exists.
     * Verifies that the deserialized object matches the sample transaction.
     */
    @Test
    void getTransactionFromRedis_ShouldReturnTransaction_WhenTransactionExists() throws Exception {
        // Arrange
        String expectedKey = CACHE_PREFIX + sampleTransaction.getId();
        String transactionJson = "{\"id\":1,\"userId\":100,\"btcPriceHistoryId\":200,\"btcAmount\":0.5," +
                "\"usdAmount\":25000.00,\"transactionTime\":\"2023-10-01T10:00:00\"," +
                "\"transactionType\":\"BUY\",\"usdBalanceBefore\":10000.00,\"btcBalanceBefore\":1.0," +
                "\"usdBalanceAfter\":7500.00,\"btcBalanceAfter\":1.5}";

        // Stub Redis to return the JSON string when the key is queried
        when(valueOperations.get(expectedKey)).thenReturn(transactionJson);

        // Stub ObjectMapper to return the sample transaction when deserializing the JSON
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

    /**
     * Tests that retrieving a transaction from Redis returns null when the transaction does not exist.
     * Verifies that deserialization is never attempted.
     */
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

    /**
     * Tests that an exception is thrown when deserialization fails during retrieval.
     * Verifies that the appropriate exception message is received.
     */
    @Test
    void getTransactionFromRedis_ShouldThrowException_WhenDeserializationFails() throws Exception {
        // Arrange
        String expectedKey = CACHE_PREFIX + sampleTransaction.getId();
        String invalidJson = "{\"id\":1,\"userId\":100,\"btcPriceHistoryId\":200,\"btcAmount\":\"invalid_amount\"," +
                "\"usdAmount\":25000.00,\"transactionTime\":\"2023-10-01T10:00:00\"," +
                "\"transactionType\":\"BUY\",\"usdBalanceBefore\":10000.00,\"btcBalanceBefore\":1.0," +
                "\"usdBalanceAfter\":7500.00,\"btcBalanceAfter\":1.5}";

        // Stub Redis to return invalid JSON
        when(valueOperations.get(expectedKey)).thenReturn(invalidJson);

        // Stub ObjectMapper to throw an exception when deserializing invalid JSON
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

    /**
     * Tests that saving a null TransactionDto throws a NullPointerException.
     * Verifies that no interaction with Redis occurs.
     */
    @Test
    void saveTransactionToRedis_ShouldHandleNullTransactionDto() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> transactionRedisService.saveTransactionToRedis(null),
                "Expected saveTransactionToRedis to throw NullPointerException when transactionDto is null");

        // The exception occurs at transactionDto.getId(), so message may vary
        // Optionally, verify no interaction with RedisTemplate
        verify(redisTemplate, never()).opsForValue();
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    /**
     * Tests that retrieving a transaction with a null ID returns null.
     * Verifies that no deserialization is attempted.
     */
    @Test
    void getTransactionFromRedis_ShouldHandleNullTransactionId() throws JsonProcessingException {
        // Arrange
        String expectedKey = CACHE_PREFIX + null;
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // Act
        TransactionDto retrievedTransaction = transactionRedisService.getTransactionFromRedis(null);

        // Assert
        assertNull(retrievedTransaction, "Retrieved transaction should be null when transactionId is null");
        verify(valueOperations, times(1)).get(expectedKey);
        verify(objectMapper, never()).readValue(anyString(), eq(TransactionDto.class));
    }

    /**
     * Tests retrieving BTC price from Redis and ensures the returned JSON is correctly deserialized.
     * This test ensures that getBTCPriceFromRedis does not return null when the key exists.
     */
    @Test
    void testGetBTCPriceFromRedis_ReturnsValue() throws Exception {
        // Arrange
        String btcPriceKey = "BTC_CURRENT_PRICE";

        // Create the DTO
        BTCPriceResponseDto priceResponseDto = new BTCPriceResponseDto(1L, new BigDecimal("50000"));

        // Serialize DTO to JSON using the real ObjectMapper
        String expectedPriceJson = objectMapper2.writeValueAsString(priceResponseDto);

        // Stub Redis to return the expected JSON string when the key is queried
        when(valueOperations.get(btcPriceKey)).thenReturn(expectedPriceJson);

        // Act
        String actualBtcPriceJson = transactionRedisService.getBTCPriceFromRedis(btcPriceKey);

        // Optional: Assert that the JSON string is not null
        assertNotNull(actualBtcPriceJson, "The returned BTC price JSON should not be null");

        // Convert the JSON string to a BTCPriceResponseDto object using the real ObjectMapper
        BTCPriceResponseDto actualBtcPrice = objectMapper2.readValue(actualBtcPriceJson, BTCPriceResponseDto.class);

        // Assert
        assertNotNull(actualBtcPrice, "The returned BTC price should not be null");
        assertEquals(priceResponseDto.getId(), actualBtcPrice.getId(), "IDs should match");
        assertEquals(priceResponseDto.getBtcPrice(), actualBtcPrice.getBtcPrice(), "BTC prices should match");

        // Verify that Redis was called correctly
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(btcPriceKey);
    }
}
