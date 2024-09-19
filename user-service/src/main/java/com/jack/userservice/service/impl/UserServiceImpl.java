package com.jack.userservice.service.impl;

import com.jack.common.constants.ErrorCode;
import com.jack.common.constants.ErrorPath;
import com.jack.common.dto.request.AuthRequestDto;
import com.jack.common.dto.request.OutboxRequestDto;
import com.jack.common.dto.response.AuthResponseDto;
import com.jack.common.dto.response.UserRegistrationDto;
import com.jack.common.dto.response.UserResponseDto;
import com.jack.common.dto.response.WalletBalanceDto;
import com.jack.common.exception.CustomErrorException;
import com.jack.userservice.client.AuthServiceClient;
import com.jack.userservice.client.OutboxServiceClient;
import com.jack.userservice.dto.UsersDto;
import com.jack.userservice.entity.Users;
import com.jack.userservice.mapper.UsersMapper;
import com.jack.userservice.repository.UsersRepository;
import com.jack.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final RedisTemplate<String, WalletBalanceDto> redisTemplate;
    private final OutboxServiceClient outboxServiceClient;

    @Value("${app.wallet.cache-prefix}")
    private String cachePrefix;

    @Value("${app.wallet.routing-key.create}")
    private String routingKey;

    @Override
    public UserResponseDto register(UserRegistrationDto registrationDTO) {
        if (usersRepository.findByEmail(registrationDTO.getEmail()).isPresent()) {
            logger.error("User registration failed. User with email '{}' already exists", registrationDTO.getEmail());
            throw new CustomErrorException(
                    ErrorCode.MAIL_ALREADY_EXISTS.getHttpStatus(),
                    ErrorCode.MAIL_ALREADY_EXISTS.getMessage(),
                    ErrorPath.POST_USER_API.getPath()
            );
        }

        String encodedPassword = passwordEncoder.encode(registrationDTO.getPassword());
        Users newUser = Users.builder()
                .name(registrationDTO.getName())
                .email(registrationDTO.getEmail())
                .password(encodedPassword)
                .build();

        Users savedUser = usersRepository.save(newUser);

        AuthRequestDto authRequest = AuthRequestDto.builder()
                .email(savedUser.getEmail())
                .password(registrationDTO.getPassword())
                .build();
        AuthResponseDto authResponse = authServiceClient.login(authRequest);

        // Prepare and send the outbox event via the outbox service
        double initialBalance = 1000.00;
        OutboxRequestDto outboxEvent = OutboxRequestDto.builder()
                .aggregateId(savedUser.getId())
                .aggregateType("User")
                .payload("{ \"userId\": " + savedUser.getId() + ", \"initialBalance\": " + initialBalance + " }")
                .routingKey(routingKey)
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
    public Users login(String email, String password) {
        logger.info("User login attempt with email: {}", email);
        Users user = findUserByEmail(email);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.error("Invalid password for email: {}", email);
            throw new CustomErrorException(
                    ErrorCode.INVALID_EMAIL_OR_PASSWORD.getHttpStatus(),
                    ErrorCode.INVALID_EMAIL_OR_PASSWORD.getMessage(),
                    ErrorPath.POST_LOGIN_API.getPath()
            );
        }

        logger.info("User with email: {} logged in successfully.", email);
        return user;
    }

    @Override
    public Optional<Users> getUserById(Long id) {
        logger.info("Fetching user by ID: {}", id);
        return Optional.of(findUserById(id));
    }

    @Override
    public Optional<Users> findByEmail(String email) {
        logger.info("Fetching user by email: {}", email);
        return usersRepository.findByEmail(email);
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

        String cacheKey = cachePrefix + userId;
        WalletBalanceDto cachedBalance = redisTemplate.opsForValue().get(cacheKey);

        if (cachedBalance != null) {
            logger.info("Returning balance from Redis for user ID: {}", userId);
            usersDTO.setUsdBalance(cachedBalance.getUsdBalance());
            usersDTO.setBtcBalance(cachedBalance.getBtcBalance());
        } else {
            logger.warn("Balance not found in Redis for user ID: {}", userId);
            usersDTO.setUsdBalance(0.0);
            usersDTO.setBtcBalance(0.0);
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
