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
import com.jack.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.jack.common.constants.EventStatus.PENDING;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthServiceClient authServiceClient;
    private final UsersMapper usersMapper;
    private final RedisTemplate<String, WalletResponseDto> redisTemplate;
    private final OutboxServiceClient outboxServiceClient;
    private final WalletServiceClient walletServiceClient;
    private final WalletBalanceRequestSender walletBalanceRequestSender;


    @Override
    public UserResponseDto register(UserRegistrationRequestDto registrationDto) {
        if (usersRepository.findByEmail(registrationDto.getEmail()).isPresent()) {
            logger.error("User registration failed. User with email '{}' already exists", registrationDto.getEmail());
            throw new CustomErrorException(
                    ErrorCode.MAIL_ALREADY_EXISTS.getHttpStatus(),
                    ErrorCode.MAIL_ALREADY_EXISTS.getMessage(),
                    ErrorPath.POST_USER_API.getPath()
            );
        }

        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());
        Users newUser = Users.builder()
                .name(registrationDto.getName())
                .email(registrationDto.getEmail())
                .password(encodedPassword)
                .build();

        Users savedUser = usersRepository.save(newUser);

        AuthRequestDto authRequest = AuthRequestDto.builder()
                .email(savedUser.getEmail())
                .password(registrationDto.getPassword())
                .build();
        AuthResponseDto authResponse = authServiceClient.login(authRequest);

        // Prepare and send the outbox event via the outbox service
        double initialBalance = 1000.00;
        OutboxRequestDto outboxEvent = OutboxRequestDto.builder()
                .aggregateId(savedUser.getId())
                .aggregateType("User")
                .payload("{ \"userId\": " + savedUser.getId() + ", \"initialBalance\": " + initialBalance + " }")
                .routingKey(WalletConstants.WALLET_CREATE_ROUTING_KEY)
                .status(PENDING.name())
                .createdAt(LocalDateTime.now().toString())
                .build();

        logger.info("Sending outbox event for user ID: {}", savedUser.getId());
        outboxServiceClient.sendOutboxEvent(outboxEvent);

        return UserResponseDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .token(authResponse.getToken())
                .build();
    }

    @Override
    public Optional<Users> updateUser(Long id, Users users) {
        logger.info("Attempting to update user with ID: {}", id);
        Users existingUser = findUserById(id);

        if (usersRepository.findByEmail(users.getEmail()).filter(user -> !user.getId().equals(id)).isPresent()) {
            logger.error("Email {} is already registered by another user.", users.getEmail());
            throw new CustomErrorException(
                    ErrorCode.MAIL_ALREADY_EXISTS.getHttpStatus(),
                    ErrorCode.MAIL_ALREADY_EXISTS.getMessage(),
                    ErrorPath.PUT_USER_API.getPath() + id
            );
        }

        existingUser.setName(users.getName());
        existingUser.setEmail(users.getEmail());

        if (users.getPassword() != null && !users.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(users.getPassword());
            existingUser.setPassword(encodedPassword);
            logger.debug("Password updated for user with ID: {}", id);
        }

        Users updatedUser = usersRepository.save(existingUser);
        logger.info("User with ID: {} updated successfully.", id);
        return Optional.of(updatedUser);
    }

    @Override
    public void deleteUser(Long id) {
        logger.info("Attempting to delete user with ID: {}", id);
        Users user = findUserById(id);
        usersRepository.delete(user);
        logger.info("User with ID: {} deleted successfully.", id);
    }

    @Override
    public boolean verifyPassword(String email, String rawPassword) {
        Users user = findUserByEmail(email);
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Override
    public UsersDto getUserWithBalance(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new CustomErrorException(
                        ErrorCode.USER_NOT_FOUND.getHttpStatus(),
                        ErrorCode.USER_NOT_FOUND.getMessage(),
                        ErrorPath.GET_USER_API.getPath() + userId
                ));

        UsersDto usersDTO = usersMapper.toDto(user);
        String cacheKey = WalletConstants.WALLET_CACHE_PREFIX + userId;

        // Step 1: Try getting balance from Redis cache first
        WalletResponseDto cachedBalance = redisTemplate.opsForValue().get(cacheKey);
        if (cachedBalance != null) {
            logger.info("Returning balance from Redis for user ID: {}", userId);
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
            logger.info("Balance for user ID {} cached in Redis.", userId);
            return usersDTO;
        } catch (Exception e) {
            logger.warn("Feign call failed for user ID {}. Sending async request to wallet-service via RabbitMQ.", userId);

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
            logger.error("User with ID: {} not found.", id);
            return new CustomErrorException(
                    ErrorCode.USER_NOT_FOUND.getHttpStatus(),
                    ErrorCode.USER_NOT_FOUND.getMessage(),
                    ErrorPath.GET_USER_API.getPath() + id
            );
        });
    }

    private Users findUserByEmail(String email) {
        return usersRepository.findByEmail(email).orElseThrow(() -> {
            logger.error("Invalid email or password for email: {}", email);
            return new CustomErrorException(
                    ErrorCode.INVALID_EMAIL_OR_PASSWORD.getHttpStatus(),
                    ErrorCode.INVALID_EMAIL_OR_PASSWORD.getMessage(),
                    ErrorPath.POST_LOGIN_API.getPath()
            );
        });
    }
}
