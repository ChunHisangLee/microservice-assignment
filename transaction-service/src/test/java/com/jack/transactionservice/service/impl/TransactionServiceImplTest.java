package com.jack.transactionservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.ApplicationConstants;
import com.jack.common.constants.TransactionConstants;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class TransactionServiceImplTest {

    @InjectMocks
    private TransactionServiceImpl transactionService;

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
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Initialize ObjectMapper as in the service
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createTransaction_Buy_Success() throws JsonProcessingException {
        // Arrange
        CreateTransactionRequestDto request = CreateTransactionRequestDto.builder()
                .userId(1L)
                .btcAmount(BigDecimal.valueOf(0.5))
                .build();

        TransactionType transactionType = TransactionType.BUY;

        BTCPriceResponseDto btcPrice = BTCPriceResponseDto.builder()
                .id(100L)
                .btcPrice(BigDecimal.valueOf(40000))
                .build();

        WalletResponseDto currentBalances = WalletResponseDto.builder()
                .userId(1L)
                .usdBalance(BigDecimal.valueOf(50000))
                .btcBalance(BigDecimal.valueOf(1.0))
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(10L)
                .userId(1L)
                .btcAmount(BigDecimal.valueOf(0.5))
                .usdAmount(BigDecimal.valueOf(20000))
                .btcPriceHistoryId(100L)
                .transactionType(TransactionType.BUY)
                .transactionTime(LocalDateTime.now())
                .build();

        TransactionDto transactionDto = TransactionDto.builder()
                .id(10L)
                .userId(1L)
                .btcAmount(BigDecimal.valueOf(0.5))
                .usdAmount(BigDecimal.valueOf(20000))
                .transactionType(TransactionType.BUY)
                .transactionTime(savedTransaction.getTransactionTime())
                .build();

        String btcPriceJson = objectMapper.writeValueAsString(btcPrice);

        when(valueOperations.get(ApplicationConstants.BTC_PRICE_KEY)).thenReturn(btcPriceJson);
        when(walletServiceClient.getWalletBalance(1L)).thenReturn(currentBalances);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toDto(any(Transaction.class), any(), any(), any(), any()))
                .thenReturn(transactionDto);

        // Act
        TransactionDto result = transactionService.createTransaction(request, transactionType);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUsdAmount()).isEqualByComparingTo("20000");

        // Verify interactions
        verify(valueOperations).get(ApplicationConstants.BTC_PRICE_KEY);
        verify(walletServiceClient).getWalletBalance(1L);
        verify(transactionRepository).save(any(Transaction.class));
        verify(outboxClient).sendTransactionEvent(10L, 1L, BigDecimal.valueOf(0.5), BigDecimal.valueOf(20000));
        verify(walletServiceClient).updateWalletBalance(1L, BigDecimal.valueOf(30000.0), BigDecimal.valueOf(1.5));
        verify(valueOperations).set(eq(TransactionConstants.TRANSACTION_CACHE_PREFIX + 10L),
                anyString(),
                eq(TransactionConstants.TRANSACTION_CACHE_TTL),
                eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    void createTransaction_Sell_Success() throws JsonProcessingException {
        // Arrange
        CreateTransactionRequestDto request = CreateTransactionRequestDto.builder()
                .userId(2L)
                .btcAmount(BigDecimal.valueOf(0.3))
                .build();

        TransactionType transactionType = TransactionType.SELL;

        BTCPriceResponseDto btcPrice = BTCPriceResponseDto.builder()
                .id(101L)
                .btcPrice(BigDecimal.valueOf(45000))
                .build();

        WalletResponseDto currentBalances = WalletResponseDto.builder()
                .userId(2L)
                .usdBalance(BigDecimal.valueOf(10000))
                .btcBalance(BigDecimal.valueOf(0.5))
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(11L)
                .userId(2L)
                .btcAmount(BigDecimal.valueOf(0.3))
                .usdAmount(BigDecimal.valueOf(13500)) // 0.3 * 45000
                .btcPriceHistoryId(101L)
                .transactionType(TransactionType.SELL)
                .transactionTime(LocalDateTime.now())
                .build();

        TransactionDto transactionDto = TransactionDto.builder()
                .id(11L)
                .userId(2L)
                .btcAmount(BigDecimal.valueOf(0.3))
                .usdAmount(BigDecimal.valueOf(13500))
                .transactionType(TransactionType.SELL)
                .transactionTime(savedTransaction.getTransactionTime())
                .build();

        String btcPriceJson = objectMapper.writeValueAsString(btcPrice);

        when(valueOperations.get(ApplicationConstants.BTC_PRICE_KEY)).thenReturn(btcPriceJson);
        when(walletServiceClient.getWalletBalance(2L)).thenReturn(currentBalances);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toDto(any(Transaction.class), any(), any(), any(), any()))
                .thenReturn(transactionDto);

        // Act
        TransactionDto result = transactionService.createTransaction(request, transactionType);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(11L);
        assertThat(result.getUsdAmount()).isEqualByComparingTo("13500");

        // Verify interactions
        verify(valueOperations).get(ApplicationConstants.BTC_PRICE_KEY);
        verify(walletServiceClient).getWalletBalance(2L);
        verify(transactionRepository).save(any(Transaction.class));
        verify(outboxClient).sendTransactionEvent(11L, 2L, BigDecimal.valueOf(0.3), BigDecimal.valueOf(13500));
        verify(walletServiceClient).updateWalletBalance(2L, BigDecimal.valueOf(23500.0), BigDecimal.valueOf(0.2));
        verify(valueOperations).set(eq(TransactionConstants.TRANSACTION_CACHE_PREFIX + 11L),
                anyString(),
                eq(TransactionConstants.TRANSACTION_CACHE_TTL),
                eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    void createTransaction_BTCPriceNotFound_ThrowsException() {
        // Arrange
        CreateTransactionRequestDto request = CreateTransactionRequestDto.builder()
                .userId(3L)
                .btcAmount(BigDecimal.valueOf(0.1))
                .build();

        TransactionType transactionType = TransactionType.BUY;

        when(valueOperations.get(ApplicationConstants.BTC_PRICE_KEY)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(request, transactionType))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BTC price not found in Redis");

        // Verify interactions
        verify(valueOperations).get(ApplicationConstants.BTC_PRICE_KEY);
        verify(walletServiceClient, never()).getWalletBalance(anyLong());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_InsufficientUSDBalance_ThrowsException() throws JsonProcessingException {
        // Arrange
        CreateTransactionRequestDto request = CreateTransactionRequestDto.builder()
                .userId(4L)
                .btcAmount(BigDecimal.valueOf(2.0))
                .build();

        TransactionType transactionType = TransactionType.BUY;

        BTCPriceResponseDto btcPrice = BTCPriceResponseDto.builder()
                .id(102L)
                .btcPrice(BigDecimal.valueOf(30000))
                .build();

        WalletResponseDto currentBalances = WalletResponseDto.builder()
                .userId(4L)
                .usdBalance(BigDecimal.valueOf(50000))
                .btcBalance(BigDecimal.valueOf(1.0))
                .build();

        String btcPriceJson = objectMapper.writeValueAsString(btcPrice);

        when(valueOperations.get(ApplicationConstants.BTC_PRICE_KEY)).thenReturn(btcPriceJson);
        when(walletServiceClient.getWalletBalance(4L)).thenReturn(currentBalances);

        // Act & Assert
        // USD needed: 2 * 30000 = 60000 > 50000
        assertThatThrownBy(() -> transactionService.createTransaction(request, transactionType))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient USD balance for this transaction.");

        // Verify interactions
        verify(valueOperations).get(ApplicationConstants.BTC_PRICE_KEY);
        verify(walletServiceClient).getWalletBalance(4L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_InsufficientBTCBalance_ThrowsException() throws JsonProcessingException {
        // Arrange
        CreateTransactionRequestDto request = CreateTransactionRequestDto.builder()
                .userId(5L)
                .btcAmount(BigDecimal.valueOf(1.5))
                .build();

        TransactionType transactionType = TransactionType.SELL;

        BTCPriceResponseDto btcPrice = BTCPriceResponseDto.builder()
                .id(103L)
                .btcPrice(BigDecimal.valueOf(35000))
                .build();

        WalletResponseDto currentBalances = WalletResponseDto.builder()
                .userId(5L)
                .usdBalance(BigDecimal.valueOf(20000))
                .btcBalance(BigDecimal.valueOf(1.0))
                .build();

        String btcPriceJson = objectMapper.writeValueAsString(btcPrice);

        when(valueOperations.get(ApplicationConstants.BTC_PRICE_KEY)).thenReturn(btcPriceJson);
        when(walletServiceClient.getWalletBalance(5L)).thenReturn(currentBalances);

        // Act & Assert
        // BTC needed to sell: 1.5 > 1.0
        assertThatThrownBy(() -> transactionService.createTransaction(request, transactionType))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient BTC balance for this transaction.");

        // Verify interactions
        verify(valueOperations).get(ApplicationConstants.BTC_PRICE_KEY);
        verify(walletServiceClient).getWalletBalance(5L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_CachingFails_ThrowsException() throws JsonProcessingException {
        // Arrange
        CreateTransactionRequestDto request = CreateTransactionRequestDto.builder()
                .userId(6L)
                .btcAmount(BigDecimal.valueOf(0.2))
                .build();

        TransactionType transactionType = TransactionType.BUY;

        BTCPriceResponseDto btcPrice = BTCPriceResponseDto.builder()
                .id(104L)
                .btcPrice(BigDecimal.valueOf(25000))
                .build();

        WalletResponseDto currentBalances = WalletResponseDto.builder()
                .userId(6L)
                .usdBalance(BigDecimal.valueOf(100000))
                .btcBalance(BigDecimal.valueOf(2.0))
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(12L)
                .userId(6L)
                .btcAmount(BigDecimal.valueOf(0.2))
                .usdAmount(BigDecimal.valueOf(5000))
                .btcPriceHistoryId(104L)
                .transactionType(TransactionType.BUY)
                .transactionTime(LocalDateTime.now())
                .build();

        String btcPriceJson = objectMapper.writeValueAsString(btcPrice);

        when(valueOperations.get(ApplicationConstants.BTC_PRICE_KEY)).thenReturn(btcPriceJson);
        when(walletServiceClient.getWalletBalance(6L)).thenReturn(currentBalances);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        doThrow(new RuntimeException("Redis set failed"))
                .when(valueOperations)
                .set(eq(TransactionConstants.TRANSACTION_CACHE_PREFIX + 12L),
                        anyString(),
                        eq(TransactionConstants.TRANSACTION_CACHE_TTL),
                        eq(java.util.concurrent.TimeUnit.MINUTES));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(request, transactionType))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize Transaction to cache");

        // Verify interactions
        verify(valueOperations).get(ApplicationConstants.BTC_PRICE_KEY);
        verify(walletServiceClient).getWalletBalance(6L);
        verify(transactionRepository).save(any(Transaction.class));
        verify(outboxClient).sendTransactionEvent(12L, 6L, BigDecimal.valueOf(0.2), BigDecimal.valueOf(5000));
        verify(walletServiceClient).updateWalletBalance(6L, BigDecimal.valueOf(95000.0), BigDecimal.valueOf(2.2));
        verify(valueOperations).set(eq(TransactionConstants.TRANSACTION_CACHE_PREFIX + 12L),
                anyString(),
                eq(TransactionConstants.TRANSACTION_CACHE_TTL),
                eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    void getUserTransactionHistory_Success() {
        // Arrange
        Long userId = 7L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by("transactionTime").descending());

        Transaction transaction1 = Transaction.builder()
                .id(13L)
                .userId(userId)
                .btcAmount(BigDecimal.valueOf(0.1))
                .usdAmount(BigDecimal.valueOf(4000))
                .transactionType(TransactionType.BUY)
                .transactionTime(LocalDateTime.now().minusDays(1))
                .build();

        Transaction transaction2 = Transaction.builder()
                .id(14L)
                .userId(userId)
                .btcAmount(BigDecimal.valueOf(0.2))
                .usdAmount(BigDecimal.valueOf(8000))
                .transactionType(TransactionType.SELL)
                .transactionTime(LocalDateTime.now())
                .build();

        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction1, transaction2));

        when(transactionRepository.findByUserId(userId, pageable)).thenReturn(transactionPage);
        when(transactionMapper.toDto(any(Transaction.class), any(), any(), any(), any()))
                .thenReturn(TransactionDto.builder().id(13L).build(),
                        TransactionDto.builder().id(14L).build());

        // Act
        Page<TransactionDto> result = transactionService.getUserTransactionHistory(userId, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(13L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(14L);

        // Verify interactions
        verify(transactionRepository).findByUserId(userId, pageable);
        verify(transactionMapper, times(2)).toDto(any(Transaction.class), any(), any(), any(), any());
    }

    @Test
    void parseBTCPriceResponse_InvalidJson_ThrowsException() {
        // Arrange
        String invalidJson = "{invalid json}";

        // To test the private method parseBTCPriceResponse, we need to use reflection or change visibility.
        // Alternatively, we can simulate the behavior via getCurrentBTCPriceFromRedis.

        CreateTransactionRequestDto request = CreateTransactionRequestDto.builder()
                .userId(8L)
                .btcAmount(BigDecimal.valueOf(0.1))
                .build();

        TransactionType transactionType = TransactionType.BUY;

        when(valueOperations.get(ApplicationConstants.BTC_PRICE_KEY)).thenReturn(invalidJson);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(request, transactionType))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to parse BTC price from Redis");

        // Verify interactions
        verify(valueOperations).get(ApplicationConstants.BTC_PRICE_KEY);
        verify(walletServiceClient, never()).getWalletBalance(anyLong());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
