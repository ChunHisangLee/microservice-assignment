package com.jack.userservice.service.impl;

import com.jack.common.constants.ErrorCode;
import com.jack.common.constants.ErrorPath;
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

import java.time.LocalDateTime;
import java.util.Optional;

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
        WalletResponseDto cachedBalance = redisTemplate.opsForValue().get(cacheKey);

        if (cachedBalance != null) {
            logger.info("Returning balance from Redis for user ID: {}", userId);
            usersDTO.setUsdBalance(cachedBalance.getUsdBalance());
            usersDTO.setBtcBalance(cachedBalance.getBtcBalance());
        } else {
            logger.warn("Balance not found in Redis for user ID: {}. Requesting from wallet-service...", userId);

            // Request the balance from wallet-service via Feign client (WalletServiceClient)
            try {
                WalletResponseDto walletBalance = walletServiceClient.getWalletBalance(userId); // Assuming this method exists in the WalletServiceClient

                // Update the DTO with the fetched balances
                usersDTO.setUsdBalance(walletBalance.getUsdBalance());
                usersDTO.setBtcBalance(walletBalance.getBtcBalance());

                // Cache the balance in Redis for future use
                redisTemplate.opsForValue().set(cacheKey, walletBalance);
                logger.info("Balance for user ID {} cached in Redis.", userId);

            } catch (Exception e) {
                logger.error("Error while fetching wallet balance from wallet-service for user ID {}: {}", userId, e.getMessage());
                throw new CustomErrorException(
                        ErrorCode.WALLET_SERVICE_ERROR.getHttpStatus(),
                        ErrorCode.WALLET_SERVICE_ERROR.getMessage(),
                        ErrorPath.GET_WALLET_BALANCE_API.getPath() + userId
                );
            }
        }

        return usersDTO;
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
