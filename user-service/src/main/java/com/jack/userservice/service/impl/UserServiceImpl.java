package com.jack.userservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.*;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.request.OutboxRequestDto;
import com.jack.common.dto.request.UserRegistrationRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import com.jack.common.dto.response.WalletResponseDto;
import com.jack.common.exception.CustomErrorException;
import com.jack.userservice.client.AuthServiceClient;
import com.jack.userservice.client.OutboxServiceClient;
import com.jack.userservice.client.WalletBalanceRequestSender;
import com.jack.userservice.client.WalletServiceClient;
import com.jack.userservice.dto.UserResponseDto;
import com.jack.userservice.dto.UserUpdateRequestDto;
import com.jack.userservice.dto.UsersDto;
import com.jack.userservice.entity.Users;
import com.jack.userservice.mapper.UsersMapper;
import com.jack.userservice.repository.UsersRepository;
import com.jack.userservice.service.UserService;
import com.jack.userservice.service.UsersRedisService;
import feign.FeignException;
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
    private final ObjectMapper objectMapper;
    private final UsersRedisService usersRedisService;
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(1000.00);

    @Override
    @Transactional
    public UserResponseDto register(UserRegistrationRequestDto registrationDto) {
        log.info("Attempting to register user with email: {}", registrationDto.getEmail());

        if (usersRepository.findByEmail(registrationDto.getEmail()).isPresent()) {
            log.error("User registration failed. User with email '{}' already exists", registrationDto.getEmail());
            throw new CustomErrorException(ErrorCode.MAIL_ALREADY_EXISTS, ErrorPath.POST_REGISTER_API.getPath());
        }

        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());

        Users newUser = Users.builder()
                .name(registrationDto.getName())
                .email(registrationDto.getEmail())
                .password(encodedPassword)
                .build();

        // Save to a database
        Users savedUser = usersRepository.save(newUser);

        // Convert to DTO and cache in Redis
        UsersDto userDto = usersMapper.toDto(savedUser);
        usersRedisService.saveUserToRedis(userDto); // Cache only

        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Authenticate user to get token
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
    public Optional<UserResponseDto> updateUser(Long id, UserUpdateRequestDto userUpdateRequestDto) {
        log.info("Attempting to update user with ID: {}", id);

        UsersDto existingUserDto;

        try {
            // Use getUserFromRedis to fetch user, checking Redis first
            existingUserDto = usersRedisService.getUserFromRedis(id);
        } catch (Exception e) {
            log.error("User with ID {} not found in updateUser", id);
            throw new CustomErrorException(ErrorCode.USER_NOT_FOUND, ErrorPath.PUT_UPDATE_USER_API.getPath());
        }

        // Convert the DTO to entity for updates
        Users existingUser = usersMapper.toEntity(existingUserDto);

        if (userUpdateRequestDto.getEmail() != null && usersRepository.findByEmail(userUpdateRequestDto.getEmail())
                .filter(user -> !user.getId().equals(id))
                .isPresent()) {
            log.error("Email {} is already registered by another user.", userUpdateRequestDto.getEmail());
            throw new CustomErrorException(ErrorCode.MAIL_ALREADY_EXISTS, ErrorPath.PUT_UPDATE_USER_API.getPath());
        }

        // Update user details
        if (userUpdateRequestDto.getName() != null) {
            existingUser.setName(userUpdateRequestDto.getName());
        }

        if (userUpdateRequestDto.getPassword() != null && !userUpdateRequestDto.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(userUpdateRequestDto.getPassword());
            existingUser.setPassword(encodedPassword);
            log.debug("Password updated for user with ID: {}", id);
        }

        Users updatedUser = usersRepository.save(existingUser);
        log.info("User with ID: {} updated successfully.", id);

        // Update the cache
        UsersDto updatedUserDto = usersMapper.toDto(updatedUser);
        usersRedisService.saveUserToRedis(updatedUserDto);

        // Map to UserResponseDto
        UserResponseDto userResponseDto = usersMapper.toResponseDto(updatedUser);

        return Optional.of(userResponseDto);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Attempting to delete user with ID: {}", id);

        // Delete user from database and invalidate cache
        usersRepository.deleteById(id);
        usersRedisService.deleteUserFromRedis(id);
        log.info("User with ID: {} deleted successfully.", id);
    }

    @Override
    public boolean isPasswordValid(String email, String rawPassword) {
        log.debug("Verifying password for email: {}", email);

        Users user = findUserByEmail(email);
        boolean isValid = passwordEncoder.matches(rawPassword, user.getPassword());
        log.debug("Password verification for email {}: {}", email, isValid);
        return isValid;
    }

    @Override
    public Optional<UsersDto> getUserWithBalance(Long userId) {
        log.info("Retrieving user with balance for user ID: {}", userId);

        UsersDto usersDTO;

        try {
            usersDTO = usersRedisService.getUserFromRedis(userId);
        } catch (Exception e) {
            log.error("User with ID {} not found in getUserWithBalance", userId);
            throw new CustomErrorException(ErrorCode.USER_NOT_FOUND, ErrorPath.GET_USER_BALANCE_API.getPath());
        }

        String cacheKey = WalletConstants.WALLET_CACHE_PREFIX + userId;

        // Step 1: Try getting balance from Redis cache first
        WalletResponseDto cachedBalance = redisTemplate.opsForValue()
                .get(cacheKey);

        if (cachedBalance != null) {
            log.info("Returning balance from Redis for user ID: {}", userId);
            usersDTO.setUsdBalance(cachedBalance.getUsdBalance());
            usersDTO.setBtcBalance(cachedBalance.getBtcBalance());
            return Optional.of(usersDTO);
        }

        // Step 2: If not in cache, try fetching from wallet-service via Feign
        try {
            WalletResponseDto walletBalance = walletServiceClient.getWalletBalance(userId);
            usersDTO.setUsdBalance(walletBalance.getUsdBalance());
            usersDTO.setBtcBalance(walletBalance.getBtcBalance());

            // Cache the balance in Redis for future requests
            redisTemplate.opsForValue()
                    .set(cacheKey, walletBalance, TransactionConstants.TRANSACTION_CACHE_TTL, TimeUnit.MINUTES);
            log.info("Balance for user ID {} cached in Redis.", userId);
            return Optional.of(usersDTO);
        } catch (Exception e) {
            log.warn("Feign call failed for user ID {}. Sending async request to wallet-service via RabbitMQ. Error: {}", userId, e.getMessage());

            // Step 3: If Feign fails, send balance request asynchronously via RabbitMQ
            walletBalanceRequestSender.sendBalanceRequest(userId);

            // Return user data with a note that balance is being fetched
            usersDTO.setUsdBalance(null); // Indicate that balance is being fetched
            usersDTO.setBtcBalance(null);
            return Optional.of(usersDTO);
        }
    }

    private Users findUserByEmail(String email) {
        return usersRepository.findByEmail(email).orElseThrow(() -> {
            log.error("Invalid email or password for email: {}", email);
            return new CustomErrorException(ErrorCode.INVALID_EMAIL_OR_PASSWORD, ErrorPath.POST_VERIFY_PASSWORD_API.getPath());
        });
    }

    private AuthResponseDto authenticateUser(String email, String password) {
        AuthRequestDto authRequest = AuthRequestDto.builder()
                .email(email)
                .password(password)
                .build();

        try {
            AuthResponseDto authResponse = authServiceClient.login(authRequest);
            log.info("User with email: {} authenticated successfully.", email);
            return authResponse;
        } catch (FeignException e) {
            log.error("Authentication failed for email: {}. Error: {}", email, e.getMessage());
            throw new CustomErrorException(ErrorCode.AUTHENTICATION_FAILED, ErrorPath.POST_LOGIN_API.getPath());
        }
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
            throw new CustomErrorException(ErrorCode.OUTBOX_EVENT_CREATION_FAILED, ErrorPath.OUTBOX_EVENT_API.getPath());
        }
    }
}
