package com.jack.transactionservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.ApplicationConstants;
import com.jack.common.constants.TransactionConstants;
import com.jack.common.dto.request.CreateTransactionRequestDto;
import com.jack.common.dto.response.BTCPriceResponseDto;
import com.jack.transactionservice.client.OutboxClient;
import com.jack.transactionservice.client.WalletServiceClient;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.Transaction;
import com.jack.transactionservice.entity.TransactionType;
import com.jack.transactionservice.mapper.TransactionMapper;
import com.jack.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private OutboxClient outboxClient;

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations; // Added ValueOperations mock

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final String BTC_PRICE_KEY = ApplicationConstants.BTC_PRICE_KEY;
    private final String TRANSACTION_CACHE_PREFIX = TransactionConstants.TRANSACTION_CACHE_PREFIX;
    private final long TRANSACTION_CACHE_TTL = TransactionConstants.TRANSACTION_CACHE_TTL;

    private TransactionDto sampleTransactionDto;
    private Transaction sampleTransaction;
    private CreateTransactionRequestDto sampleCreateRequest;
    private BTCPriceResponseDto sampleBTCPrice;

    @BeforeEach
    void setUp() {
        // Initialize sample TransactionDto
        sampleTransactionDto = TransactionDto.builder()
                .id(1L)
                .userId(100L)
                .btcPriceHistoryId(200L)
                .btcAmount(new BigDecimal("0.5"))
                .usdAmount(new BigDecimal("25000.00"))
                .transactionTime(LocalDateTime.of(2023, 10, 1, 10, 0))
                .transactionType(TransactionType.BUY)
                .usdBalanceBefore(new BigDecimal("10000.00"))
                .btcBalanceBefore(new BigDecimal("1.0"))
                .usdBalanceAfter(new BigDecimal("7500.00"))
                .btcBalanceAfter(new BigDecimal("1.5"))
                .build();

        // Initialize sample Transaction entity
        sampleTransaction = Transaction.builder()
                .id(1L)
                .userId(100L)
                .btcPriceHistoryId(200L)
                .btcAmount(new BigDecimal("0.5"))
                .usdAmount(new BigDecimal("25000.00"))
                .transactionTime(LocalDateTime.of(2023, 10, 1, 10, 0))
                .transactionType(TransactionType.BUY)
                .build();

        // Initialize sample CreateTransactionRequestDto
        sampleCreateRequest = CreateTransactionRequestDto.builder()
                .userId(100L)
                .btcAmount(new BigDecimal("0.5"))
                .usdAmount(new BigDecimal("2500.00"))
                .usdBalanceBefore(new BigDecimal("10000.00"))
                .btcBalanceBefore(new BigDecimal("1.0"))
                .usdBalanceAfter(new BigDecimal("7500.00"))
                .btcBalanceAfter(new BigDecimal("1.5"))
                .build();

        // Initialize sample BTCPriceResponseDto
        sampleBTCPrice = BTCPriceResponseDto.builder()
                .id(200L)
                .btcPrice(new BigDecimal("50000.00"))
                .build();

        // Set up RedisTemplate to return the mocked ValueOperations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createTransaction_ShouldCreateTransactionSuccessfully() throws Exception {
        // Arrange
        String btcPriceJson = "{\"BTC_CURRENT_PRICE\":50000.00}";

        // Ensure objectMapper is mocked correctly
        when(valueOperations.get(BTC_PRICE_KEY)).thenReturn(btcPriceJson);

        // Mock the deserialization to return a properly initialized DTO
        BTCPriceResponseDto sampleBTCPrice = new BTCPriceResponseDto();
        sampleBTCPrice.setId(200L);
        sampleBTCPrice.setBtcPrice(new BigDecimal("50000.00"));
        when(objectMapper.readValue(anyString(), eq(BTCPriceResponseDto.class))).thenReturn(sampleBTCPrice);

        // Continue mocking other dependencies
        when(objectMapper.writeValueAsString(any(Transaction.class))).thenReturn("{\"id\":1,\"userId\":100,\"btcAmount\":0.5}");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);
        doNothing().when(outboxClient).sendTransactionEvent(anyLong(), anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        doNothing().when(walletServiceClient).updateWalletBalance(anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        when(transactionMapper.toDto(eq(sampleTransaction),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(sampleTransactionDto);

        // Act
        TransactionDto result = transactionService.createTransaction(sampleCreateRequest, TransactionType.BUY);

        // Assert
        assertNotNull(result, "TransactionDto should not be null");
        assertEquals(sampleTransactionDto.getId(), result.getId(), "Transaction ID should match");
        assertEquals(sampleTransactionDto.getUserId(), result.getUserId(), "User ID should match");
        assertEquals(sampleTransactionDto.getUsdAmount(), result.getUsdAmount(), "USD Amount should match");

        // Verify interactions
        verify(valueOperations, times(1)).get(BTC_PRICE_KEY);
        verify(objectMapper, times(1)).readValue(btcPriceJson, BTCPriceResponseDto.class);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(outboxClient, times(1)).sendTransactionEvent(sampleTransaction.getId(),
                sampleCreateRequest.getUserId(),
                sampleCreateRequest.getBtcAmount(),
                sampleTransaction.getUsdAmount());
        verify(walletServiceClient, times(1)).updateWalletBalance(eq(sampleCreateRequest.getUserId()),
                eq(new BigDecimal("7500.00")), eq(new BigDecimal("1.5")));
        verify(objectMapper, times(1)).writeValueAsString(any(Transaction.class));
        verify(valueOperations, times(1)).set(eq(TRANSACTION_CACHE_PREFIX + sampleTransaction.getId()),
                eq("{\"id\":1,\"userId\":100,\"btcAmount\":0.5}"), eq(TRANSACTION_CACHE_TTL), eq(TimeUnit.MINUTES));
        verify(transactionMapper, times(1)).toDto(eq(sampleTransaction),
                eq(sampleCreateRequest.getUsdBalanceBefore()),
                eq(sampleCreateRequest.getBtcBalanceBefore()),
                eq(new BigDecimal("7500.00")),
                eq(new BigDecimal("1.5")));
    }

    @Test
    void createTransaction_ShouldThrowException_WhenBTCPriceNotFoundInRedis() {
        // Arrange
        when(valueOperations.get(BTC_PRICE_KEY)).thenReturn(null); // BTC price not found

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                        transactionService.createTransaction(sampleCreateRequest, TransactionType.BUY),
                "Expected createTransaction to throw IllegalStateException when BTC price is not found in Redis"
        );

        assertEquals("BTC price not found in Redis", exception.getMessage());

        // Verify interactions
        verify(valueOperations, times(1)).get(BTC_PRICE_KEY);

        // Ensure no further interactions
        verifyNoMoreInteractions(transactionRepository, outboxClient, walletServiceClient, transactionMapper, objectMapper, valueOperations);
    }

    @Test
    void createTransaction_ShouldThrowException_WhenSerializationFailsDuringCaching() throws Exception {
        // Arrange
        String btcPriceJson = "{\"id\":200,\"btcPrice\":50000.00}";
        when(valueOperations.get(BTC_PRICE_KEY)).thenReturn(btcPriceJson);
        when(objectMapper.readValue(anyString(), eq(BTCPriceResponseDto.class))).thenReturn(sampleBTCPrice);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);
        doNothing().when(outboxClient).sendTransactionEvent(anyLong(), anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        doNothing().when(walletServiceClient).updateWalletBalance(anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        when(objectMapper.writeValueAsString(any(Transaction.class))).thenThrow(new JsonProcessingException("Serialization Error") {});

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                        transactionService.createTransaction(sampleCreateRequest, TransactionType.BUY),
                "Expected createTransaction to throw IllegalStateException when serialization fails during caching"
        );

        assertTrue(exception.getMessage().contains("Failed to serialize Transaction to cache"));

        // Verify interactions
        verify(valueOperations, times(1)).get(BTC_PRICE_KEY);
        verify(objectMapper, times(1)).readValue(btcPriceJson, BTCPriceResponseDto.class);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(outboxClient, times(1)).sendTransactionEvent(sampleTransaction.getId(),
                sampleCreateRequest.getUserId(),
                sampleCreateRequest.getBtcAmount(),
                sampleTransaction.getUsdAmount());
        verify(walletServiceClient, times(1)).updateWalletBalance(eq(sampleCreateRequest.getUserId()),
                eq(new BigDecimal("7500.00")), eq(new BigDecimal("1.5")));
        verify(objectMapper, times(1)).writeValueAsString(any(Transaction.class));
        // Since serialization fails, 'set' should never be called
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), eq(TimeUnit.MINUTES));
        verify(transactionMapper, never()).toDto(any(), any(), any(), any(), any());
    }

    @Test
    void getUserTransactionHistory_ShouldReturnTransactions_WhenTransactionsExist() {
        // Arrange
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by("transactionTime").descending());

        Transaction transaction1 = Transaction.builder()
                .id(1L)
                .userId(userId)
                .btcPriceHistoryId(200L)
                .btcAmount(new BigDecimal("0.5"))
                .usdAmount(new BigDecimal("25000.00"))
                .transactionTime(LocalDateTime.of(2023, 10, 1, 10, 0))
                .transactionType(TransactionType.BUY)
                .build();

        Transaction transaction2 = Transaction.builder()
                .id(2L)
                .userId(userId)
                .btcPriceHistoryId(201L)
                .btcAmount(new BigDecimal("0.3"))
                .usdAmount(new BigDecimal("15000.00"))
                .transactionTime(LocalDateTime.of(2023, 10, 2, 11, 0))
                .transactionType(TransactionType.SELL)
                .build();

        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(transaction1, transaction2), pageable, 2);

        when(transactionRepository.findByUserId(userId, pageable)).thenReturn(transactionPage);

        // Mocking mapper to return DTOs
        TransactionDto dto1 = TransactionDto.builder()
                .id(transaction1.getId())
                .userId(transaction1.getUserId())
                .btcPriceHistoryId(transaction1.getBtcPriceHistoryId())
                .btcAmount(transaction1.getBtcAmount())
                .usdAmount(transaction1.getUsdAmount())
                .transactionTime(transaction1.getTransactionTime())
                .transactionType(transaction1.getTransactionType())
                .usdBalanceBefore(new BigDecimal("10000.00"))
                .btcBalanceBefore(new BigDecimal("1.0"))
                .usdBalanceAfter(new BigDecimal("7500.00"))
                .btcBalanceAfter(new BigDecimal("1.5"))
                .build();

        TransactionDto dto2 = TransactionDto.builder()
                .id(transaction2.getId())
                .userId(transaction2.getUserId())
                .btcPriceHistoryId(transaction2.getBtcPriceHistoryId())
                .btcAmount(transaction2.getBtcAmount())
                .usdAmount(transaction2.getUsdAmount())
                .transactionTime(transaction2.getTransactionTime())
                .transactionType(transaction2.getTransactionType())
                .usdBalanceBefore(new BigDecimal("7500.00"))
                .btcBalanceBefore(new BigDecimal("1.5"))
                .usdBalanceAfter(new BigDecimal("9000.00"))
                .btcBalanceAfter(new BigDecimal("1.2"))
                .build();

        when(transactionMapper.toDto(eq(transaction1),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(dto1);
        when(transactionMapper.toDto(eq(transaction2),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(dto2);

        // Act
        Page<TransactionDto> result = transactionService.getUserTransactionHistory(userId, pageable);

        // Assert
        assertNotNull(result, "Resulting page should not be null");
        assertEquals(2, result.getTotalElements(), "Total elements should be 2");
        assertEquals(1, result.getTotalPages(), "Total pages should be 1");
        assertEquals(2, result.getContent().size(), "Content size should be 2");
        assertEquals(dto1, result.getContent().get(0), "First transaction DTO should match");
        assertEquals(dto2, result.getContent().get(1), "Second transaction DTO should match");

        // Verify interactions
        verify(transactionRepository, times(1)).findByUserId(userId, pageable);
        verify(transactionMapper, times(1)).toDto(eq(transaction1),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class));
        verify(transactionMapper, times(1)).toDto(eq(transaction2),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class));
    }

    @Test
    void getUserTransactionHistory_ShouldReturnEmptyPage_WhenNoTransactionsExist() {
        // Arrange
        Long userId = 101L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by("transactionTime").descending());

        Page<Transaction> transactionPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(transactionRepository.findByUserId(userId, pageable)).thenReturn(transactionPage);

        // Act
        Page<TransactionDto> result = transactionService.getUserTransactionHistory(userId, pageable);

        // Assert
        assertNotNull(result, "Resulting page should not be null");
        assertTrue(result.isEmpty(), "Resulting page should be empty");
        assertEquals(0, result.getTotalElements(), "Total elements should be 0");
        assertEquals(0, result.getTotalPages(), "Total pages should be 0");

        // Verify interactions
        verify(transactionRepository, times(1)).findByUserId(userId, pageable);
        verify(transactionMapper, never()).toDto(any(), any(), any(), any(), any());
    }

    @Test
    void createTransaction_ShouldThrowException_WhenWalletServiceFails() throws Exception {
        // Arrange
        String btcPriceJson = "{\"id\":200,\"btcPrice\":50000.00}";
        when(valueOperations.get(BTC_PRICE_KEY)).thenReturn(btcPriceJson);
        when(objectMapper.readValue(anyString(), eq(BTCPriceResponseDto.class))).thenReturn(sampleBTCPrice);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);
        doNothing().when(outboxClient).sendTransactionEvent(anyLong(), anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        doThrow(new RuntimeException("WalletService is down")).when(walletServiceClient).updateWalletBalance(anyLong(), any(BigDecimal.class), any(BigDecimal.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                        transactionService.createTransaction(sampleCreateRequest, TransactionType.BUY),
                "Expected createTransaction to throw RuntimeException when WalletService fails"
        );

        assertEquals("WalletService is down", exception.getMessage());

        // Verify interactions
        verify(valueOperations, times(1)).get(BTC_PRICE_KEY);
        verify(objectMapper, times(1)).readValue(btcPriceJson, BTCPriceResponseDto.class);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(outboxClient, times(1)).sendTransactionEvent(sampleTransaction.getId(),
                sampleCreateRequest.getUserId(),
                sampleCreateRequest.getBtcAmount(),
                sampleTransaction.getUsdAmount());
        verify(walletServiceClient, times(1)).updateWalletBalance(eq(sampleCreateRequest.getUserId()),
                eq(new BigDecimal("7500.00")), eq(new BigDecimal("1.5")));
        verify(objectMapper, times(1)).writeValueAsString(any(Transaction.class));
        // Since wallet update failed, caching should not occur
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), eq(TimeUnit.MINUTES));
        verify(transactionMapper, never()).toDto(any(), any(), any(), any(), any());
    }

    @Test
    void createTransaction_ShouldThrowException_WhenRequestDtoIsNull() throws Exception {
        // Arrange
        when(valueOperations.get(BTC_PRICE_KEY)).thenReturn(null); // BTC price not found

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                        transactionService.createTransaction(null, TransactionType.BUY),
                "Expected createTransaction to throw IllegalStateException when BTC price is not found in Redis"
        );

        assertEquals("BTC price not found in Redis", exception.getMessage());

        // Verify interactions
        verify(valueOperations, times(1)).get(BTC_PRICE_KEY);
        verify(objectMapper, never()).readValue(anyString(), eq(BTCPriceResponseDto.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(outboxClient, never()).sendTransactionEvent(anyLong(), anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        verify(walletServiceClient, never()).updateWalletBalance(anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        verify(objectMapper, never()).writeValueAsString(any(Transaction.class));
        verify(transactionMapper, never()).toDto(any(), any(), any(), any(), any());
    }
}
