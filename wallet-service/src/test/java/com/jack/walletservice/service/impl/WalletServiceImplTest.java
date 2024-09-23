package com.jack.walletservice.service.impl;

import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.common.dto.response.WalletResponseDto;
import com.jack.walletservice.entity.Wallet;
import com.jack.walletservice.exception.WalletNotFoundException;
import com.jack.walletservice.publisher.WalletBalancePublisher;
import com.jack.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private RedisTemplate<String, WalletResponseDto> redisTemplate;

    @Mock
    private WalletBalancePublisher walletBalancePublisher;

    @InjectMocks
    private WalletServiceImpl walletService;

    // Define the cache prefix used in the service
    private static final String WALLET_CACHE_PREFIX = "walletBalance:";

    @Test
    void testCreateWalletSuccess() {
        // Arrange
        Long userId = 1L;
        BigDecimal initialBalance = BigDecimal.valueOf(1000);
        WalletCreateMessageDto message = new WalletCreateMessageDto(userId, initialBalance);

        Wallet savedWallet = new Wallet();
        savedWallet.setUserId(userId);
        savedWallet.setUsdBalance(initialBalance);
        savedWallet.setBtcBalance(BigDecimal.ZERO);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // Mock Redis operations
        ValueOperations<String, WalletResponseDto> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        walletService.createWallet(message);

        // Assert
        verify(walletRepository, times(1)).findByUserId(userId);
        verify(walletRepository, times(1)).save(any(Wallet.class));
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).set(eq(WALLET_CACHE_PREFIX + userId), any(WalletResponseDto.class));
        verify(walletBalancePublisher, times(1)).publishWalletBalance(any(WalletResponseDto.class));
    }

    @Test
    void testCreateWalletAlreadyExists() {
        // Arrange
        Long userId = 1L;
        BigDecimal initialBalance = BigDecimal.valueOf(1000);
        WalletCreateMessageDto message = new WalletCreateMessageDto(userId, initialBalance);

        Wallet existingWallet = new Wallet();
        existingWallet.setUserId(userId);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(existingWallet));

        // Act
        walletService.createWallet(message);

        // Assert
        verify(walletRepository, times(1)).findByUserId(userId);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(redisTemplate, never()).opsForValue();
        verify(walletBalancePublisher, never()).publishWalletBalance(any(WalletResponseDto.class));
    }

    @Test
    void testUpdateWalletSuccess() {
        // Arrange
        Long userId = 1L;
        BigDecimal usdAmount = BigDecimal.valueOf(100);
        BigDecimal btcAmount = BigDecimal.valueOf(0.5);

        Wallet existingWallet = new Wallet();
        existingWallet.setUserId(userId);
        existingWallet.setUsdBalance(BigDecimal.valueOf(1000));
        existingWallet.setBtcBalance(BigDecimal.valueOf(1));

        Wallet updatedWallet = new Wallet();
        updatedWallet.setUserId(userId);
        updatedWallet.setUsdBalance(existingWallet.getUsdBalance().add(usdAmount));
        updatedWallet.setBtcBalance(existingWallet.getBtcBalance().add(btcAmount));

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(existingWallet)).thenReturn(updatedWallet);

        // Mock Redis operations
        ValueOperations<String, WalletResponseDto> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        walletService.updateWallet(userId, usdAmount, btcAmount);

        // Assert
        verify(walletRepository, times(1)).findByUserId(userId);
        verify(walletRepository, times(1)).save(existingWallet);
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).set(eq(WALLET_CACHE_PREFIX + userId), any(WalletResponseDto.class));
        verify(walletBalancePublisher, times(1)).publishWalletBalance(any(WalletResponseDto.class));
    }

    @Test
    void testUpdateWalletNotFound() {
        // Arrange
        Long userId = 1L;
        BigDecimal usdAmount = BigDecimal.valueOf(100);
        BigDecimal btcAmount = BigDecimal.valueOf(0.5);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        WalletNotFoundException exception = assertThrows(WalletNotFoundException.class, () ->
                walletService.updateWallet(userId, usdAmount, btcAmount)
        );

        assertEquals("Wallet not found for user ID: " + userId, exception.getMessage());

        verify(walletRepository, times(1)).findByUserId(userId);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(redisTemplate, never()).opsForValue();
        verify(walletBalancePublisher, never()).publishWalletBalance(any(WalletResponseDto.class));
    }

    @Test
    void testGetWalletBalanceCacheHit() {
        // Arrange
        Long userId = 1L;
        WalletResponseDto cachedWallet = WalletResponseDto.builder()
                .userId(userId)
                .usdBalance(BigDecimal.valueOf(1000))
                .btcBalance(BigDecimal.valueOf(1))
                .build();

        // Mock Redis operations
        ValueOperations<String, WalletResponseDto> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WALLET_CACHE_PREFIX + userId)).thenReturn(cachedWallet);

        // Act
        WalletResponseDto result = walletService.getWalletBalance(userId);

        // Assert
        assertNotNull(result);
        assertEquals(cachedWallet, result);
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(WALLET_CACHE_PREFIX + userId);
        verify(walletRepository, never()).findByUserId(anyLong());
        verify(walletBalancePublisher, never()).publishWalletBalance(any(WalletResponseDto.class));
        verify(valueOperations, never()).set(anyString(), any(WalletResponseDto.class));
    }

    @Test
    void testGetWalletBalanceCacheMiss() {
        // Arrange
        Long userId = 1L;
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setUsdBalance(BigDecimal.valueOf(1000));
        wallet.setBtcBalance(BigDecimal.valueOf(1));

        WalletResponseDto updatedCache = WalletResponseDto.builder()
                .userId(userId)
                .usdBalance(wallet.getUsdBalance())
                .btcBalance(wallet.getBtcBalance())
                .build();

        // Mock Redis operations
        ValueOperations<String, WalletResponseDto> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WALLET_CACHE_PREFIX + userId)).thenReturn(null); // Cache miss
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        // Act
        WalletResponseDto result = walletService.getWalletBalance(userId);

        // Assert
        assertNotNull(result);
        assertEquals(wallet.getUserId(), result.getUserId());
        assertEquals(wallet.getUsdBalance(), result.getUsdBalance());
        assertEquals(wallet.getBtcBalance(), result.getBtcBalance());

        verify(redisTemplate, times(2)).opsForValue(); // Once for get, once for set
        verify(valueOperations, times(1)).get(WALLET_CACHE_PREFIX + userId);
        verify(walletRepository, times(1)).findByUserId(userId);
        verify(valueOperations, times(1)).set(eq(WALLET_CACHE_PREFIX + userId), any(WalletResponseDto.class));
        verify(walletBalancePublisher, times(1)).publishWalletBalance(any(WalletResponseDto.class));
    }

    @Test
    void testGetWalletBalanceNotFound() {
        // Arrange
        Long userId = 1L;

        // Mock Redis operations
        ValueOperations<String, WalletResponseDto> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WALLET_CACHE_PREFIX + userId)).thenReturn(null); // Cache miss
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        WalletNotFoundException exception = assertThrows(WalletNotFoundException.class, () ->
                walletService.getWalletBalance(userId)
        );

        assertEquals("Wallet not found for user ID: " + userId, exception.getMessage());

        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(WALLET_CACHE_PREFIX + userId);
        verify(walletRepository, times(1)).findByUserId(userId);
        verify(valueOperations, never()).set(anyString(), any(WalletResponseDto.class));
        verify(walletBalancePublisher, never()).publishWalletBalance(any(WalletResponseDto.class));
    }
}
