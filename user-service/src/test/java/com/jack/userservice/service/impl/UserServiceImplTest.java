package com.jack.userservice.service.impl;

import com.jack.common.constants.ErrorCode;
import com.jack.common.constants.ErrorPath;
import com.jack.common.constants.TransactionConstants;
import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.request.OutboxRequestDto;
import com.jack.common.dto.request.UserRegistrationRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import com.jack.common.dto.response.UserResponseDto;
import com.jack.common.dto.response.WalletResponseDto;
import com.jack.common.exception.CustomErrorException;
import com.jack.userservice.client.AuthServiceClient;
import com.jack.userservice.client.OutboxServiceClient;
import com.jack.userservice.client.WalletBalanceRequestSender;
import com.jack.userservice.client.WalletServiceClient;
import com.jack.userservice.dto.UsersDto;
import com.jack.userservice.entity.Users;
import com.jack.userservice.mapper.UsersMapper;
import com.jack.userservice.repository.UsersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.jack.common.constants.EventStatus.PENDING;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private UsersMapper usersMapper;

    @Mock
    private RedisTemplate<String, WalletResponseDto> redisTemplate;

    @Mock
    private OutboxServiceClient outboxServiceClient;

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private WalletBalanceRequestSender walletBalanceRequestSender;

    @InjectMocks
    private UserServiceImpl userService;

    // Define the cache prefix used in the service
    private static final String WALLET_CACHE_PREFIX = "walletBalance:";

    /**
     * Test successful user registration.
     */
    @Test
    void testRegisterSuccess() {
        // Arrange
        Long userId = 1L;
        String name = "John Doe";
        String email = "john.doe@example.com";
        String rawPassword = "password123";
        double initialBalance = 1000.00;

        UserRegistrationRequestDto registrationDto = new UserRegistrationRequestDto(name, email, rawPassword);

        // Stub: User does not exist
        when(usersRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Stub: Password encoding
        String encodedPassword = "encodedPassword123";
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        // Stub: Saving the user
        Users savedUser = Users.builder()
                .id(userId)
                .name(name)
                .email(email)
                .password(encodedPassword)
                .build();
        when(usersRepository.save(any(Users.class))).thenReturn(savedUser);

        // Prepare AuthRequestDto (will not be used directly in stubbing)
        AuthRequestDto authRequest = AuthRequestDto.builder()
                .email(email)
                .password(rawPassword)
                .build();

        // Stub: AuthServiceClient.login using argument matcher
        AuthResponseDto authResponse = AuthResponseDto.builder()
                .token("authToken123")
                .build();
        when(authServiceClient.login(any(AuthRequestDto.class))).thenReturn(authResponse);

        // Act
        UserResponseDto responseDto = userService.register(registrationDto);

        // Assert
        verify(usersRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(usersRepository, times(1)).save(any(Users.class));
        verify(authServiceClient, times(1)).login(any(AuthRequestDto.class)); // Changed to use argument matcher

        // Capture and verify the OutboxRequestDto
        ArgumentCaptor<OutboxRequestDto> outboxCaptor = ArgumentCaptor.forClass(OutboxRequestDto.class);
        verify(outboxServiceClient, times(1)).sendOutboxEvent(outboxCaptor.capture());

        OutboxRequestDto capturedOutbox = outboxCaptor.getValue();
        assertEquals(userId, capturedOutbox.getAggregateId());
        assertEquals("User", capturedOutbox.getAggregateType());
        String expectedPayload = "{ \"userId\": " + userId + ", \"initialBalance\": " + initialBalance + " }";
        assertEquals(expectedPayload, capturedOutbox.getPayload());
        assertEquals(WalletConstants.WALLET_CREATE_ROUTING_KEY, capturedOutbox.getRoutingKey());
        assertEquals(PENDING.name(), capturedOutbox.getStatus());
        // Optionally, verify the format of createdAt (e.g., non-null, valid date-time format)

        // Verify the response DTO
        assertNotNull(responseDto);
        assertEquals(userId, responseDto.getId());
        assertEquals(email, responseDto.getEmail());
        assertEquals(authResponse.getToken(), responseDto.getToken());
    }


    /**
     * Test registration failure when email already exists.
     */
    @Test
    void testRegisterEmailAlreadyExists() {
        // Arrange
        String name = "John Doe";
        String email = "john.doe@example.com";
        String rawPassword = "password123";

        UserRegistrationRequestDto registrationDto = new UserRegistrationRequestDto(name, email, rawPassword);

        Users existingUser = Users.builder()
                .id(1L)
                .name(name)
                .email(email)
                .password("existingEncodedPassword")
                .build();

        when(usersRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

        // Act & Assert
        CustomErrorException exception = assertThrows(CustomErrorException.class, () ->
                userService.register(registrationDto)
        );

        assertEquals(ErrorCode.MAIL_ALREADY_EXISTS.getMessage(), exception.getMessage());
        // Adjust based on the actual getter method in CustomErrorException
        assertEquals(ErrorCode.MAIL_ALREADY_EXISTS.getHttpStatus().value(), exception.getStatusCode());
        assertEquals(ErrorPath.POST_USER_API.getPath(), exception.getPath());

        verify(usersRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, never()).encode(anyString());
        verify(usersRepository, never()).save(any(Users.class));
        verify(authServiceClient, never()).login(any(AuthRequestDto.class));
        verify(outboxServiceClient, never()).sendOutboxEvent(any(OutboxRequestDto.class));
    }

    /**
     * Test successful user update.
     */
    @Test
    void testUpdateUserSuccess() {
        // Arrange
        Long userId = 1L;
        String newName = "Jane Doe";
        String newEmail = "jane.doe@example.com";
        String newPassword = "newPassword123";

        Users existingUser = Users.builder()
                .id(userId)
                .name("John Doe")
                .email("john.doe@example.com")
                .password("encodedOldPassword")
                .build();

        Users updatedUser = Users.builder()
                .id(userId)
                .name(newName)
                .email(newEmail)
                .password("encodedNewPassword")
                .build();

        Users updateRequest = Users.builder()
                .name(newName)
                .email(newEmail)
                .password(newPassword)
                .build();

        when(usersRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(usersRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(usersRepository.save(existingUser)).thenReturn(updatedUser);

        // Act
        Optional<Users> result = userService.updateUser(userId, updateRequest);

        // Assert
        assertTrue(result.isPresent());
        Users returnedUser = result.get();
        assertEquals(newName, returnedUser.getName());
        assertEquals(newEmail, returnedUser.getEmail());
        assertEquals("encodedNewPassword", returnedUser.getPassword());

        verify(usersRepository, times(1)).findById(userId);
        verify(usersRepository, times(1)).findByEmail(newEmail);
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(usersRepository, times(1)).save(existingUser);
    }

    /**
     * Test user update failure when new email already exists for another user.
     */
    @Test
    void testUpdateUserEmailAlreadyExists() {
        // Arrange
        Long userId = 1L;
        String newName = "Jane Doe";
        String newEmail = "jane.doe@example.com";
        String newPassword = "newPassword123";

        Users existingUser = Users.builder()
                .id(userId)
                .name("John Doe")
                .email("john.doe@example.com")
                .password("encodedOldPassword")
                .build();

        Users anotherUser = Users.builder()
                .id(2L)
                .name("Alice")
                .email(newEmail)
                .password("encodedAlicePassword")
                .build();

        Users updateRequest = Users.builder()
                .name(newName)
                .email(newEmail)
                .password(newPassword)
                .build();

        when(usersRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(usersRepository.findByEmail(newEmail)).thenReturn(Optional.of(anotherUser));

        // Act & Assert
        CustomErrorException exception = assertThrows(CustomErrorException.class, () ->
                userService.updateUser(userId, updateRequest)
        );

        assertEquals(ErrorCode.MAIL_ALREADY_EXISTS.getMessage(), exception.getMessage());
        assertEquals(ErrorCode.MAIL_ALREADY_EXISTS.getHttpStatus().value(), exception.getStatusCode());
        assertEquals(ErrorPath.PUT_USER_API.getPath() + userId, exception.getPath());

        verify(usersRepository, times(1)).findById(userId);
        verify(usersRepository, times(1)).findByEmail(newEmail);
        verify(passwordEncoder, never()).encode(anyString());
        verify(usersRepository, never()).save(any(Users.class));
    }

    /**
     * Test user update failure when user is not found.
     */
    @Test
    void testUpdateUserNotFound() {
        // Arrange
        Long userId = 1L;
        Users updateRequest = Users.builder()
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .password("newPassword123")
                .build();

        when(usersRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        CustomErrorException exception = assertThrows(CustomErrorException.class, () ->
                userService.updateUser(userId, updateRequest)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), exception.getMessage());
        assertEquals(ErrorCode.USER_NOT_FOUND.getHttpStatus().value(), exception.getStatusCode());
        assertEquals(ErrorPath.GET_USER_API.getPath() + userId, exception.getPath());

        verify(usersRepository, times(1)).findById(userId);
        verify(usersRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(usersRepository, never()).save(any(Users.class));
    }

    /**
     * Test successful user deletion.
     */
    @Test
    void testDeleteUserSuccess() {
        // Arrange
        Long userId = 1L;

        Users existingUser = Users.builder()
                .id(userId)
                .name("John Doe")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .build();

        when(usersRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(usersRepository, times(1)).findById(userId);
        verify(usersRepository, times(1)).delete(existingUser);
    }

    /**
     * Test user deletion failure when user is not found.
     */
    @Test
    void testDeleteUserNotFound() {
        // Arrange
        Long userId = 1L;

        when(usersRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        CustomErrorException exception = assertThrows(CustomErrorException.class, () ->
                userService.deleteUser(userId)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), exception.getMessage());
        assertEquals(ErrorCode.USER_NOT_FOUND.getHttpStatus().value(), exception.getStatusCode());
        assertEquals(ErrorPath.GET_USER_API.getPath() + userId, exception.getPath());

        verify(usersRepository, times(1)).findById(userId);
        verify(usersRepository, never()).delete(any(Users.class));
    }

    /**
     * Test successful password verification.
     */
    @Test
    void testVerifyPasswordSuccess() {
        // Arrange
        String email = "john.doe@example.com";
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword123";

        Users user = Users.builder()
                .id(1L)
                .name("John Doe")
                .email(email)
                .password(encodedPassword)
                .build();

        when(usersRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        // Act
        boolean result = userService.verifyPassword(email, rawPassword);

        // Assert
        assertTrue(result);
        verify(usersRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, times(1)).matches(rawPassword, encodedPassword);
    }

    /**
     * Test password verification failure due to incorrect password.
     */
    @Test
    void testVerifyPasswordIncorrect() {
        // Arrange
        String email = "john.doe@example.com";
        String rawPassword = "wrongPassword";
        String encodedPassword = "encodedPassword123";

        Users user = Users.builder()
                .id(1L)
                .name("John Doe")
                .email(email)
                .password(encodedPassword)
                .build();

        when(usersRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        // Act
        boolean result = userService.verifyPassword(email, rawPassword);

        // Assert
        assertFalse(result);
        verify(usersRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, times(1)).matches(rawPassword, encodedPassword);
    }

    /**
     * Test password verification failure when user is not found.
     */
    @Test
    void testVerifyPasswordUserNotFound() {
        // Arrange
        String email = "john.doe@example.com";
        String rawPassword = "password123";

        when(usersRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        CustomErrorException exception = assertThrows(CustomErrorException.class, () ->
                userService.verifyPassword(email, rawPassword)
        );

        assertEquals(ErrorCode.INVALID_EMAIL_OR_PASSWORD.getMessage(), exception.getMessage());
        assertEquals(ErrorCode.INVALID_EMAIL_OR_PASSWORD.getHttpStatus().value(), exception.getStatusCode());
        assertEquals(ErrorPath.POST_LOGIN_API.getPath(), exception.getPath());

        verify(usersRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    /**
     * Test getUserWithBalance with cache hit.
     */
    @Test
    void testGetUserWithBalanceCacheHit() {
        // Arrange
        Long userId = 1L;
        String name = "John Doe";
        String email = "john.doe@example.com";

        Users user = Users.builder()
                .id(userId)
                .name(name)
                .email(email)
                .password("encodedPassword")
                .build();

        UsersDto usersDto = UsersDto.builder()
                .id(userId)
                .name(name)
                .email(email)
                .usdBalance(BigDecimal.valueOf(1000))
                .btcBalance(BigDecimal.valueOf(1))
                .build();

        WalletResponseDto cachedWallet = WalletResponseDto.builder()
                .userId(userId)
                .usdBalance(BigDecimal.valueOf(1000))
                .btcBalance(BigDecimal.valueOf(1))
                .build();

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));

        // Mock Redis operations
        ValueOperations<String, WalletResponseDto> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WALLET_CACHE_PREFIX + userId)).thenReturn(cachedWallet);

        // Mock UsersMapper to convert Users to UsersDto
        when(usersMapper.toDto(user)).thenReturn(usersDto);

        // Act
        UsersDto result = userService.getUserWithBalance(userId);

        // Assert
        assertNotNull(result);
        assertEquals(usersDto.getId(), result.getId());
        assertEquals(usersDto.getName(), result.getName());
        assertEquals(usersDto.getEmail(), result.getEmail());
        assertEquals(usersDto.getUsdBalance(), result.getUsdBalance());
        assertEquals(usersDto.getBtcBalance(), result.getBtcBalance());

        verify(usersRepository, times(1)).findById(userId);
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(WALLET_CACHE_PREFIX + userId);
        verify(walletServiceClient, never()).getWalletBalance(anyLong());
        verify(walletBalanceRequestSender, never()).sendBalanceRequest(anyLong());
    }

    /**
     * Test getUserWithBalance with cache miss and Feign call success.
     */
    @Test
    void testGetUserWithBalanceCacheMissFeignSuccess() {
        // Arrange
        Long userId = 1L;
        String name = "John Doe";
        String email = "john.doe@example.com";

        Users user = Users.builder()
                .id(userId)
                .name(name)
                .email(email)
                .password("encodedPassword")
                .build();

        UsersDto usersDto = UsersDto.builder()
                .id(userId)
                .name(name)
                .email(email)
                .build();

        WalletResponseDto walletBalance = WalletResponseDto.builder()
                .userId(userId)
                .usdBalance(BigDecimal.valueOf(1000))
                .btcBalance(BigDecimal.valueOf(1))
                .build();

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(usersMapper.toDto(user)).thenReturn(usersDto);
        when(walletServiceClient.getWalletBalance(userId)).thenReturn(walletBalance);

        // Mock Redis operations
        ValueOperations<String, WalletResponseDto> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WALLET_CACHE_PREFIX + userId)).thenReturn(null); // Cache miss

        // Act
        UsersDto result = userService.getUserWithBalance(userId);

        // Assert
        assertNotNull(result);
        assertEquals(usersDto.getId(), result.getId());
        assertEquals(usersDto.getName(), result.getName());
        assertEquals(usersDto.getEmail(), result.getEmail());
        assertEquals(walletBalance.getUsdBalance(), result.getUsdBalance());
        assertEquals(walletBalance.getBtcBalance(), result.getBtcBalance());

        verify(usersRepository, times(1)).findById(userId);
        verify(redisTemplate, times(2)).opsForValue(); // Once for get, once for set
        verify(valueOperations, times(1)).get(WALLET_CACHE_PREFIX + userId);
        verify(walletServiceClient, times(1)).getWalletBalance(userId);
        verify(valueOperations, times(1)).set(WALLET_CACHE_PREFIX + userId, walletBalance, TransactionConstants.TRANSACTION_CACHE_TTL, TimeUnit.MINUTES);
        verify(walletBalanceRequestSender, never()).sendBalanceRequest(anyLong());
    }

    /**
     * Test getUserWithBalance with cache miss and Feign call failure.
     */
    @Test
    void testGetUserWithBalanceCacheMissFeignFailure() {
        // Arrange
        Long userId = 1L;
        String name = "John Doe";
        String email = "john.doe@example.com";

        Users user = Users.builder()
                .id(userId)
                .name(name)
                .email(email)
                .password("encodedPassword")
                .build();

        UsersDto usersDto = UsersDto.builder()
                .id(userId)
                .name(name)
                .email(email)
                .usdBalance(BigDecimal.ZERO)
                .btcBalance(BigDecimal.ZERO)
                .build();

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(usersMapper.toDto(user)).thenReturn(usersDto);

        // Mock Redis operations
        ValueOperations<String, WalletResponseDto> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WALLET_CACHE_PREFIX + userId)).thenReturn(null); // Cache miss

        // Mock Feign call to throw exception
        when(walletServiceClient.getWalletBalance(userId)).thenThrow(new RuntimeException("Feign client error"));

        // Act
        UsersDto result = userService.getUserWithBalance(userId);

        // Assert
        assertNotNull(result);
        assertEquals(usersDto.getId(), result.getId());
        assertEquals(usersDto.getName(), result.getName());
        assertEquals(usersDto.getEmail(), result.getEmail());
        assertEquals(BigDecimal.ZERO, result.getUsdBalance());
        assertEquals(BigDecimal.ZERO, result.getBtcBalance());

        verify(usersRepository, times(1)).findById(userId);
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(WALLET_CACHE_PREFIX + userId);
        verify(walletServiceClient, times(1)).getWalletBalance(userId);
        verify(walletBalanceRequestSender, times(1)).sendBalanceRequest(userId);
        verify(valueOperations, never()).set(anyString(), any(WalletResponseDto.class));
    }

    /**
     * Test getUserWithBalance when user is not found.
     */
    @Test
    void testGetUserWithBalanceUserNotFound() {
        // Arrange
        Long userId = 1L;

        when(usersRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        CustomErrorException exception = assertThrows(CustomErrorException.class, () ->
                userService.getUserWithBalance(userId)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), exception.getMessage());
        assertEquals(ErrorCode.USER_NOT_FOUND.getHttpStatus().value(), exception.getStatusCode());
        assertEquals(ErrorPath.GET_USER_API.getPath() + userId, exception.getPath());

        verify(usersRepository, times(1)).findById(userId);
        verify(redisTemplate, never()).opsForValue();
        verify(walletServiceClient, never()).getWalletBalance(anyLong());
        verify(walletBalanceRequestSender, never()).sendBalanceRequest(anyLong());
    }
}
