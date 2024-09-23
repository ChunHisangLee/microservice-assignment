package com.jack.transactionservice.service.impl;

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
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {

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

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private CreateTransactionRequestDto requestDto;
    private BTCPriceResponseDto btcPriceResponseDto;
    private Transaction savedTransaction;
    private TransactionDto expectedTransactionDto;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        // Initialize ObjectMapper as it's used internally
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // Initialize CreateTransactionRequestDto
        requestDto = new CreateTransactionRequestDto();
        requestDto.setId(9527L);
        requestDto.setUserId(100L);
        requestDto.setBtcAmount(new BigDecimal("0.01"));
        requestDto.setUsdBalanceBefore(new BigDecimal("10000"));
        requestDto.setBtcBalanceBefore(new BigDecimal("0.5"));

        // Initialize BTCPriceResponseDto
        btcPriceResponseDto = new BTCPriceResponseDto();
        btcPriceResponseDto.setId(200L);
        btcPriceResponseDto.setBtcPrice(new BigDecimal("50000.00"));

        // Initialize saved Transaction
        savedTransaction = Transaction.builder()
                .id(9527L)
                .userId(100L)
                .btcPriceHistoryId(200L)
                .btcAmount(new BigDecimal("0.01"))
                .usdAmount(new BigDecimal("500.00"))
                .transactionTime(LocalDateTime.now())
                .transactionType(TransactionType.BUY)
                .build();

        // Initialize expected TransactionDto
        expectedTransactionDto = new TransactionDto();
        expectedTransactionDto.setId(9527L);
        expectedTransactionDto.setUserId(100L);
        expectedTransactionDto.setBtcPriceHistoryId(200L);
        expectedTransactionDto.setBtcAmount(new BigDecimal("0.01"));
        expectedTransactionDto.setUsdAmount(new BigDecimal("500.00"));
        expectedTransactionDto.setTransactionTime(savedTransaction.getTransactionTime());
        expectedTransactionDto.setTransactionType(TransactionType.BUY);
        expectedTransactionDto.setUsdBalanceAfter(new BigDecimal("9500.00"));
        expectedTransactionDto.setBtcBalanceAfter(new BigDecimal("0.51"));
    }

    /**
         * Custom ArgumentMatcher for BigDecimal to ignore scale differences.
         */
        private record BigDecimalMatcher(BigDecimal expected) implements ArgumentMatcher<BigDecimal> {

        @Override
            public boolean matches(BigDecimal actual) {
                if (actual == null) {
                    return false;
                }
                return actual.compareTo(expected) == 0;
            }

            @Override
            public String toString() {
                return "BigDecimalMatcher{" +
                        "expected=" + expected +
                        '}';
            }
        }

    @Test
    public void createTransaction_ShouldCreateTransactionSuccessfully() throws Exception {
        // Arrange

        // Mock Redis response for BTC price
        String btcPriceJson = objectMapper.writeValueAsString(btcPriceResponseDto);
        when(redisTemplate.opsForValue().get(ApplicationConstants.BTC_PRICE_KEY)).thenReturn(btcPriceJson);

        // Mock transactionRepository.save to return the savedTransaction
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Mock transactionMapper.toDto with custom BigDecimal matchers
        when(transactionMapper.toDto(
                eq(savedTransaction),
                any(BigDecimal.class),
                any(BigDecimal.class),
                argThat(new BigDecimalMatcher(new BigDecimal("9500.00"))),
                argThat(new BigDecimalMatcher(new BigDecimal("0.51")))
        )).thenReturn(expectedTransactionDto);

        // Act
        TransactionDto actualDto = transactionService.createTransaction(requestDto, TransactionType.BUY);

        // Assert
        assertThat(actualDto).isNotNull();
        assertThat(actualDto).isEqualTo(expectedTransactionDto);

        // Verify that redisTemplate was called to get BTC price
        verify(redisTemplate, times(2)).opsForValue(); // Called twice: once in getCurrentBTCPriceFromRedis and once in getCurrentBtcPrice
        verify(redisTemplate, times(1)).opsForValue().get(ApplicationConstants.BTC_PRICE_KEY);

        // Verify that transactionRepository.save was called once
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(transactionCaptor.capture());

        Transaction capturedTransaction = transactionCaptor.getValue();
        assertThat(capturedTransaction.getUserId()).isEqualTo(100);
        assertThat(capturedTransaction.getBtcAmount()).isEqualTo(0.01);
        assertThat(capturedTransaction.getUsdAmount()).isEqualByComparingTo("500.00");
        assertThat(capturedTransaction.getBtcPriceHistoryId()).isEqualTo(200);
        assertThat(capturedTransaction.getTransactionType()).isEqualTo(TransactionType.BUY);
        assertThat(capturedTransaction.getTransactionTime()).isNotNull();

        // Verify that outboxClient.sendTransactionEvent was called once with correct arguments
        verify(outboxClient, times(1)).sendTransactionEvent(
                eq(savedTransaction.getId()),
                eq(100L),
                BigDecimal.valueOf(eq(0.01)),
                eq(new BigDecimal("500.00"))
        );

        // Verify that walletServiceClient.updateWalletBalance was called once with correct arguments
        verify(walletServiceClient, times(1)).updateWalletBalance(
                eq(100L),
                argThat(new BigDecimalMatcher(new BigDecimal("9500.00"))),
                argThat(new BigDecimalMatcher(new BigDecimal("0.51")))
        );

        // Verify that transactionMapper.toDto was called once with correct arguments
        verify(transactionMapper, times(1)).toDto(
                eq(savedTransaction),
                eq(new BigDecimal("10000")),
                eq(new BigDecimal("0.5")),
                argThat(new BigDecimalMatcher(new BigDecimal("9500.00"))),
                argThat(new BigDecimalMatcher(new BigDecimal("0.51")))
        );

        // Verify that cacheTransaction was called by verifying redisTemplate.opsForValue().set
        verify(redisTemplate, times(1)).opsForValue().set(
                eq(TransactionConstants.TRANSACTION_CACHE_PREFIX + savedTransaction.getId()),
                anyString(),
                eq(TransactionConstants.TRANSACTION_CACHE_TTL),
                eq(TimeUnit.MINUTES)
        );

        // Verify no more interactions
        verifyNoMoreInteractions(transactionRepository, transactionMapper, outboxClient, walletServiceClient, redisTemplate);
    }
}
