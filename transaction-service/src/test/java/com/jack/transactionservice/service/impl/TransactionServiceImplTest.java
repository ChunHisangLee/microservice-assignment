package com.jack.transactionservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.ApplicationConstants;
import com.jack.common.dto.request.CreateTransactionRequestDto;
import com.jack.common.dto.response.BTCPriceResponseDto;
import com.jack.common.dto.response.WalletResponseDto;
import com.jack.transactionservice.client.OutboxClient;
import com.jack.transactionservice.client.WalletServiceClient;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.Transaction;
import com.jack.transactionservice.entity.TransactionType;
import com.jack.transactionservice.mapper.TransactionMapper;
import com.jack.transactionservice.repository.TransactionRepository;
import com.jack.transactionservice.service.TransactionRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private OutboxClient outboxClient;

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private TransactionRedisService transactionRedisService;

    /**
     * Using a Spy for ObjectMapper to allow real method calls while still being able to stub specific methods if needed.
     */
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private CreateTransactionRequestDto createTransactionRequestDto;
    private BTCPriceResponseDto btcPriceResponseDto;
    private WalletResponseDto walletResponseDto;
    private Transaction sampleTransaction;
    private TransactionDto sampleTransactionDto;

    /**
     * Sets up the test environment before each test.
     * Initializes sample data and configures necessary stubbings.
     */
    @BeforeEach
    void setUp() throws JsonProcessingException {
        // Initialize a sample CreateTransactionRequestDto
        createTransactionRequestDto = CreateTransactionRequestDto.builder()
                .userId(100L)
                .btcAmount(BigDecimal.valueOf(0.5))
                .build();

        // Initialize a sample BTCPriceResponseDto
        btcPriceResponseDto = new BTCPriceResponseDto(1L, BigDecimal.valueOf(500));

        // Initialize a sample WalletResponseDto
        walletResponseDto = new WalletResponseDto(100L, BigDecimal.valueOf(1000.00), BigDecimal.valueOf(1.0));

        // Initialize a sample Transaction entity
        sampleTransaction = Transaction.builder()
                .id(1L)
                .userId(100L)
                .btcPriceHistoryId(1L)
                .btcAmount(BigDecimal.valueOf(0.5))
                .usdAmount(BigDecimal.valueOf(250.00))
                .transactionTime(LocalDateTime.now())
                .transactionType(TransactionType.BUY)
                .build();

        // Initialize a sample TransactionDto
        sampleTransactionDto = TransactionDto.builder()
                .id(1L)
                .userId(100L)
                .btcPriceHistoryId(200L)
                .btcAmount(BigDecimal.valueOf(0.5))
                .usdAmount(BigDecimal.valueOf(250.00))
                .transactionTime(LocalDateTime.of(2023, 10, 1, 10, 0))
                .transactionType(TransactionType.BUY)
                .usdBalanceBefore(BigDecimal.valueOf(1000.00))
                .btcBalanceBefore(BigDecimal.valueOf(1.0))
                .usdBalanceAfter(BigDecimal.valueOf(750.00))
                .btcBalanceAfter(BigDecimal.valueOf(1.5))
                .build();

        // Inject the spied ObjectMapper into the service
        ReflectionTestUtils.setField(transactionService, "objectMapper", objectMapper);
    }

    /**
     * Tests the successful creation of a transaction.
     */
    @Test
    void createTransaction_ShouldCreateTransactionSuccessfully() throws Exception {
        // Arrange

        // Stub transactionRedisService.getBTCPriceFromRedis to return BTCPriceResponseDto as JSON
        String btcPriceJson = objectMapper.writeValueAsString(btcPriceResponseDto);
        when(transactionRedisService.getBTCPriceFromRedis(ApplicationConstants.BTC_PRICE_KEY)).thenReturn(btcPriceJson);

        // Use doReturn() for stubbing methods on the Spy ObjectMapper
        doReturn(btcPriceResponseDto).when(objectMapper).readValue(btcPriceJson, BTCPriceResponseDto.class);

        // Stub walletServiceClient.getWalletBalance
        when(walletServiceClient.getWalletBalance(createTransactionRequestDto.getUserId())).thenReturn(walletResponseDto);

        // Use doReturn() for stubbing methods on the Spy ObjectMapper
        doReturn("{\"id\":1,\"userId\":100,\"btcPriceHistoryId\":1,\"btcAmount\":0.5,\"usdAmount\":250.00,\"transactionTime\":\"2023-10-01T10:00:00\",\"transactionType\":\"BUY\",\"usdBalanceBefore\":100.00,\"btcBalanceBefore\":1.0,\"usdBalanceAfter\":75.00,\"btcBalanceAfter\":1.5}")
                .when(objectMapper).writeValueAsString(any(TransactionDto.class));

        // Stub transactionRepository.save
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        // Stub transactionMapper.toDto
        when(transactionMapper.toDto(any(Transaction.class), any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class))).thenReturn(sampleTransactionDto);

        // Act
        TransactionDto result = transactionService.createTransaction(createTransactionRequestDto, TransactionType.BUY);

        // Assert
        assertNotNull(result, "The returned TransactionDto should not be null");
        assertEquals(sampleTransactionDto.getId(), result.getId(), "Transaction ID should match");
        assertEquals(sampleTransactionDto.getUserId(), result.getUserId(), "User ID should match");
        assertEquals(sampleTransactionDto.getBtcAmount(), result.getBtcAmount(), "BTC Amount should match");
        assertEquals(sampleTransactionDto.getUsdAmount(), result.getUsdAmount(), "USD Amount should match");
        assertEquals(sampleTransactionDto.getTransactionType(), result.getTransactionType(), "Transaction Type should match");
        assertEquals(sampleTransactionDto.getUsdBalanceAfter(), result.getUsdBalanceAfter(), "USD Balance After should match");
        assertEquals(sampleTransactionDto.getBtcBalanceAfter(), result.getBtcBalanceAfter(), "BTC Balance After should match");

        // Verify interactions
        verify(transactionRedisService, times(1)).getBTCPriceFromRedis(ApplicationConstants.BTC_PRICE_KEY);
        verify(objectMapper, times(1)).readValue(btcPriceJson, BTCPriceResponseDto.class);
        verify(walletServiceClient, times(1)).getWalletBalance(createTransactionRequestDto.getUserId());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(outboxClient, times(1)).sendTransactionEvent(sampleTransaction.getId(),
                sampleTransaction.getUserId(), sampleTransaction.getBtcAmount(), sampleTransaction.getUsdAmount());
        verify(walletServiceClient, times(1)).updateWalletBalance(createTransactionRequestDto.getUserId(),
                sampleTransactionDto.getUsdBalanceAfter(), sampleTransactionDto.getBtcBalanceAfter());
        verify(transactionRedisService, times(1)).saveTransactionToRedis(sampleTransactionDto);

        // **Update the verification count to 2**
        verify(transactionMapper, times(2)).toDto(any(Transaction.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    /**
     * Tests that creating a transaction throws an exception when BTC price is not found in Redis.
     */
    @Test
    void createTransaction_ShouldThrowException_WhenBTCPriceNotFound() {
        // Arrange
        when(transactionRedisService.getBTCPriceFromRedis(ApplicationConstants.BTC_PRICE_KEY)).thenReturn(null);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                        transactionService.createTransaction(createTransactionRequestDto, TransactionType.BUY),
                "Expected createTransaction to throw, but it didn't");

        assertEquals("BTC price not found in Redis", exception.getMessage(), "Exception message should match");

        // Verify interactions
        verify(transactionRedisService, times(1)).getBTCPriceFromRedis(ApplicationConstants.BTC_PRICE_KEY);
        verify(walletServiceClient, never()).getWalletBalance(anyLong());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(outboxClient, never()).sendTransactionEvent(anyLong(), anyLong(), any(), any());
        verify(walletServiceClient, never()).updateWalletBalance(anyLong(), any(), any());
        verify(transactionRedisService, never()).saveTransactionToRedis(any(TransactionDto.class));
        verify(transactionMapper, never()).toDto(any(Transaction.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    /**
     * Tests that creating a transaction throws an exception when updating wallet balances fails.
     */
    @Test
    void createTransaction_ShouldThrowException_WhenWalletUpdateFails() throws Exception {
        // Arrange

        // Stub transactionRedisService.getBTCPriceFromRedis
        String btcPriceJson = objectMapper.writeValueAsString(btcPriceResponseDto);
        when(transactionRedisService.getBTCPriceFromRedis(ApplicationConstants.BTC_PRICE_KEY)).thenReturn(btcPriceJson);

        // Stub objectMapper.readValue for BTCPriceResponseDto
        when(objectMapper.readValue(btcPriceJson, BTCPriceResponseDto.class)).thenReturn(btcPriceResponseDto);

        // Stub walletServiceClient.getWalletBalance
        when(walletServiceClient.getWalletBalance(createTransactionRequestDto.getUserId())).thenReturn(walletResponseDto);

        // Stub objectMapper.writeValueAsString for TransactionDto
        when(objectMapper.writeValueAsString(any(TransactionDto.class))).thenReturn("{\"id\":1,\"userId\":100,\"btcPriceHistoryId\":1,\"btcAmount\":0.5,\"usdAmount\":25000.00,\"transactionTime\":\"2023-10-01T10:00:00\",\"transactionType\":\"BUY\",\"usdBalanceBefore\":10000.00,\"btcBalanceBefore\":1.0,\"usdBalanceAfter\":7500.00,\"btcBalanceAfter\":1.5}");

        // Stub transactionRepository.save
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        // Stub transactionMapper.toDto
        when(transactionMapper.toDto(any(Transaction.class), any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class))).thenReturn(sampleTransactionDto);

        // Stub walletServiceClient.updateWalletBalance to throw exception
        doThrow(new RuntimeException("Wallet update failed"))
                .when(walletServiceClient).updateWalletBalance(anyLong(), any(BigDecimal.class), any(BigDecimal.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                        transactionService.createTransaction(createTransactionRequestDto, TransactionType.BUY),
                "Expected createTransaction to throw, but it didn't");

        assertEquals("Wallet update failed", exception.getMessage(), "Exception message should match");

        // Verify interactions
        verify(transactionRedisService, times(1)).getBTCPriceFromRedis(ApplicationConstants.BTC_PRICE_KEY);
        verify(objectMapper, times(1)).readValue(btcPriceJson, BTCPriceResponseDto.class);
        verify(walletServiceClient, times(1)).getWalletBalance(createTransactionRequestDto.getUserId());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(outboxClient, times(1)).sendTransactionEvent(sampleTransaction.getId(),
                sampleTransaction.getUserId(), sampleTransaction.getBtcAmount(), sampleTransaction.getUsdAmount());
        verify(walletServiceClient, times(1)).updateWalletBalance(createTransactionRequestDto.getUserId(),
                sampleTransactionDto.getUsdBalanceAfter(), sampleTransactionDto.getBtcBalanceAfter());
        verify(transactionRedisService, times(1)).saveTransactionToRedis(sampleTransactionDto);
        verify(transactionMapper, times(1)).toDto(any(Transaction.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    /**
     * Tests retrieving a transaction by ID when it exists in cache.
     */
    @Test
    void getTransactionById_ShouldReturnCachedTransaction_WhenExistsInCache() {
        // Arrange
        Long transactionId = 1L;
        when(transactionRedisService.getTransactionFromRedis(transactionId)).thenReturn(sampleTransactionDto);

        // Act
        TransactionDto result = transactionService.getTransactionById(transactionId);

        // Assert
        assertNotNull(result, "The returned TransactionDto should not be null");
        assertEquals(sampleTransactionDto.getId(), result.getId(), "Transaction ID should match");
        assertEquals(sampleTransactionDto.getUserId(), result.getUserId(), "User ID should match");

        // Verify interactions
        verify(transactionRedisService, times(1)).getTransactionFromRedis(transactionId);
        verify(transactionRepository, never()).findById(anyLong());
        verify(transactionRedisService, never()).saveTransactionToRedis(any(TransactionDto.class));
        verify(transactionMapper, never()).toDto(any(Transaction.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    /**
     * Tests retrieving a transaction by ID when it does not exist in cache but exists in the database.
     */
    @Test
    void getTransactionById_ShouldReturnTransactionFromDB_WhenNotInCache() {
        // Arrange
        Long transactionId = 1L;
        when(transactionRedisService.getTransactionFromRedis(transactionId)).thenReturn(null);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(sampleTransaction));
        when(transactionMapper.toDto(sampleTransaction, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .thenReturn(sampleTransactionDto);

        // Act
        TransactionDto result = transactionService.getTransactionById(transactionId);

        // Assert
        assertNotNull(result, "The returned TransactionDto should not be null");
        assertEquals(sampleTransactionDto.getId(), result.getId(), "Transaction ID should match");
        assertEquals(sampleTransactionDto.getUserId(), result.getUserId(), "User ID should match");

        // Verify interactions
        verify(transactionRedisService, times(1)).getTransactionFromRedis(transactionId);
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRedisService, times(1)).saveTransactionToRedis(sampleTransactionDto);
        verify(transactionMapper, times(1)).toDto(sampleTransaction, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * Tests retrieving a transaction by ID when it does not exist in both cache and database.
     */
    @Test
    void getTransactionById_ShouldThrowException_WhenTransactionNotFound() {
        // Arrange
        Long transactionId = 1L;
        when(transactionRedisService.getTransactionFromRedis(transactionId)).thenReturn(null);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                        transactionService.getTransactionById(transactionId),
                "Expected getTransactionById to throw, but it didn't");

        assertEquals("Transaction not found with ID: " + transactionId, exception.getMessage(), "Exception message should match");

        // Verify interactions
        verify(transactionRedisService, times(1)).getTransactionFromRedis(transactionId);
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRedisService, never()).saveTransactionToRedis(any(TransactionDto.class));
        verify(transactionMapper, never()).toDto(any(Transaction.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    /**
     * Tests fetching user transaction history successfully.
     */
    @Test
    void getUserTransactionHistory_ShouldReturnTransactionPage() {
        // Arrange
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by("transactionTime").descending());
        Transaction transaction1 = Transaction.builder().id(1L).userId(userId).build();
        Transaction transaction2 = Transaction.builder().id(2L).userId(userId).build();
        List<Transaction> transactions = Arrays.asList(transaction1, transaction2);
        Page<Transaction> transactionPage = new PageImpl<>(transactions, pageable, transactions.size());

        when(transactionRepository.findByUserId(userId, pageable)).thenReturn(transactionPage);
        when(transactionMapper.toDto(any(Transaction.class), any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class))).thenReturn(new TransactionDto());

        // Act
        Page<TransactionDto> result = transactionService.getUserTransactionHistory(userId, pageable);

        // Assert
        assertNotNull(result, "The returned Page<TransactionDto> should not be null");
        assertEquals(2, result.getTotalElements(), "Total elements should match");
        assertEquals(1, result.getTotalPages(), "Total pages should match");
        assertEquals(2, result.getContent().size(), "Content size should match");

        // Verify interactions
        verify(transactionRepository, times(1)).findByUserId(userId, pageable);
        verify(transactionMapper, times(2)).toDto(any(Transaction.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    /**
     * Tests creating a transaction when deserialization of BTC price fails.
     */
    @Test
    void createTransaction_ShouldThrowException_WhenDeserializationFails() throws Exception {
        // Arrange
        String btcPriceKey = ApplicationConstants.BTC_PRICE_KEY;
        String invalidBtcPriceJson = "invalidJson";
        when(transactionRedisService.getBTCPriceFromRedis(btcPriceKey)).thenReturn(invalidBtcPriceJson);
        when(objectMapper.readValue(invalidBtcPriceJson, BTCPriceResponseDto.class))
                .thenThrow(new JsonProcessingException("Deserialization error") {
                });

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                        transactionService.createTransaction(createTransactionRequestDto, TransactionType.BUY),
                "Expected createTransaction to throw, but it didn't");

        assertEquals("Failed to parse BTC price from Redis: Deserialization error", exception.getMessage(), "Exception message should match");

        // Verify interactions
        verify(transactionRedisService, times(1)).getBTCPriceFromRedis(btcPriceKey);
        verify(objectMapper, times(1)).readValue(invalidBtcPriceJson, BTCPriceResponseDto.class);
        verify(walletServiceClient, never()).getWalletBalance(anyLong());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(outboxClient, never()).sendTransactionEvent(anyLong(), anyLong(), any(), any());
        verify(walletServiceClient, never()).updateWalletBalance(anyLong(), any(), any());
        verify(transactionRedisService, never()).saveTransactionToRedis(any(TransactionDto.class));
        verify(transactionMapper, never()).toDto(any(Transaction.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class));
    }
}
