package com.jack.userservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.*;
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
import com.jack.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserServiceImpl implements UserService {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthServiceClient authServiceClient;
    private final UsersMapper usersMapper;
    private final RedisTemplate<String, WalletResponseDto> redisTemplate;
    private final OutboxServiceClient outboxServiceClient;
    private final WalletServiceClient walletServiceClient;
    private final WalletBalanceRequestSender walletBalanceRequestSender;
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(1000.00);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public UserResponseDto register(UserRegistrationRequestDto registrationDto) {
        if (usersRepository.findByEmail(registrationDto.getEmail()).isPresent()) {
            log.error("User registration failed. User with email '{}' already exists", registrationDto.getEmail());
            throw createErrorResponse(ErrorCode.MAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());
        Users newUser = Users.builder()
                .name(registrationDto.getName())
                .email(registrationDto.getEmail())
                .password(encodedPassword)
                .build();

        Users savedUser = usersRepository.save(newUser);
        AuthResponseDto authResponse = authenticateUser(savedUser.getEmail(), registrationDto.getPassword());

        // Prepare and send the outbox event via the outbox service
        OutboxRequestDto outboxEvent = createOutboxEvent(savedUser.getId());
        log.info("Sending outbox event for user ID: {}", savedUser.getId());
        outboxServiceClient.sendOutboxEvent(outboxEvent);

        return UserResponseDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .token(authResponse.getToken())
                .build();
    }

    @Override
    @Transactional
    public Optional<Users> updateUser(Long id, Users users) {
        log.info("Attempting to update user with ID: {}", id);
        Users existingUser = findUserById(id);

        if (usersRepository.findByEmail(users.getEmail()).filter(user -> !user.getId().equals(id)).isPresent()) {
            log.error("Email {} is already registered by another user.", users.getEmail());
            throw createErrorResponse(ErrorCode.MAIL_ALREADY_EXISTS);
        }

        existingUser.setName(users.getName());
        existingUser.setEmail(users.getEmail());

        if (users.getPassword() != null && !users.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(users.getPassword());
            existingUser.setPassword(encodedPassword);
            log.debug("Password updated for user with ID: {}", id);
        }

        Users updatedUser = usersRepository.save(existingUser);
        log.info("User with ID: {} updated successfully.", id);
        return Optional.of(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Attempting to delete user with ID: {}", id);
        Users user = findUserById(id);
        usersRepository.delete(user);
        log.info("User with ID: {} deleted successfully.", id);
    }

    @Override
    public boolean verifyPassword(String email, String rawPassword) {
        Users user = findUserByEmail(email);
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Override
    public UsersDto getUserWithBalance(Long userId) {
        Users user = findUserById(userId);
        UsersDto usersDTO = usersMapper.toDto(user);
        String cacheKey = WalletConstants.WALLET_CACHE_PREFIX + userId;

        // Step 1: Try getting balance from Redis cache first
        WalletResponseDto cachedBalance = redisTemplate.opsForValue()
                .get(cacheKey);

        if (cachedBalance != null) {
            log.info("Returning balance from Redis for user ID: {}", userId);
            usersDTO.setUsdBalance(cachedBalance.getUsdBalance());
            usersDTO.setBtcBalance(cachedBalance.getBtcBalance());
            return usersDTO;
        }

        // Step 2: If not in cache, try fetching from wallet-service via Feign
        try {
            WalletResponseDto walletBalance = walletServiceClient.getWalletBalance(userId);
            usersDTO.setUsdBalance(walletBalance.getUsdBalance());
            usersDTO.setBtcBalance(walletBalance.getBtcBalance());

            // Cache the balance in Redis for future requests
            redisTemplate.opsForValue().set(cacheKey, walletBalance, TransactionConstants.TRANSACTION_CACHE_TTL, TimeUnit.MINUTES);
            log.info("Balance for user ID {} cached in Redis.", userId);
            return usersDTO;
        } catch (Exception e) {
            log.warn("Feign call failed for user ID {}. Sending async request to wallet-service via RabbitMQ.", userId);

            // Step 3: If Feign fails, send balance request asynchronously via RabbitMQ
            walletBalanceRequestSender.sendBalanceRequest(userId);

            // Optionally, return partial data and wait for the async response to arrive
            usersDTO.setUsdBalance(BigDecimal.ZERO);  // Default/fallback values
            usersDTO.setBtcBalance(BigDecimal.ZERO);
            return usersDTO;
        }
    }

    private Users findUserById(Long id) {
        return usersRepository.findById(id).orElseThrow(() -> {
            log.error("User with ID: {} not found.", id);
            return createErrorResponse(ErrorCode.USER_NOT_FOUND);
        });
    }

    private Users findUserByEmail(String email) {
        return usersRepository.findByEmail(email).orElseThrow(() -> {
            log.error("Invalid email or password for email: {}", email);
            return createErrorResponse(ErrorCode.INVALID_EMAIL_OR_PASSWORD);
        });
    }

    private CustomErrorException createErrorResponse(ErrorCode errorCode) {
        return new CustomErrorException(
                errorCode.getHttpStatus(),
                errorCode.getMessage(),
                ErrorPath.POST_LOGIN_API.getPath()
        );
    }

    private AuthResponseDto authenticateUser(String email, String password) {
        AuthRequestDto authRequest = AuthRequestDto.builder()
                .email(email)
                .password(password)
                .build();
        return authServiceClient.login(authRequest);
    }

    private OutboxRequestDto createOutboxEvent(Long userId) {
        try {
            // Create a map to hold the data
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("userId", userId);
            payloadMap.put("initialBalance", UserServiceImpl.INITIAL_BALANCE);

            // Convert the map to a JSON string
            String payload = objectMapper.writeValueAsString(payloadMap);

            // Create and return the OutboxRequestDto
            return OutboxRequestDto.builder()
                    .aggregateId(userId.toString())  // Adjusted to String as per your Outbox entity definition
                    .aggregateType("User")            // Set the aggregate type
                    .eventType("USER_CREATED")         // Specify the event type
                    .payload(payload)                  // Use the serialized JSON payload
                    .routingKey(WalletConstants.WALLET_CREATE_ROUTING_KEY) // Routing key for the outbox event
                    .status(EventStatus.PENDING)       // Set status to PENDING
                    .createdAt(LocalDateTime.now())    // Set creation timestamp
                    .sequenceNumber(1L)                // Placeholder for sequence number; update logic as needed
                    .eventId(UUID.randomUUID().toString()) // Generate a unique event ID
                    .build();
        } catch (JsonProcessingException e) {
            // Handle the exception (you could log it and rethrow or wrap in a custom exception)
            log.error("Failed to create JSON payload for outbox event: {}", e.getMessage());
            throw new RuntimeException("Error creating outbox event payload", e);
        }
    }
}
