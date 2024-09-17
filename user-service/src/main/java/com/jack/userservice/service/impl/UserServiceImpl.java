package com.jack.userservice.service.impl;

import com.jack.common.dto.response.UserRegistrationDto;
import com.jack.common.dto.response.UserResponseDto;
import com.jack.common.dto.response.WalletBalanceDto;
import com.jack.common.exception.CustomErrorException;
import com.jack.userservice.client.AuthServiceClient;
import com.jack.userservice.dto.UsersDTO;
import com.jack.userservice.entity.Users;
import com.jack.common.entity.Outbox;
import com.jack.common.constants.EventStatus;
import com.jack.userservice.mapper.UsersMapper;
import com.jack.userservice.repository.OutboxRepository;
import com.jack.userservice.repository.UsersRepository;
import com.jack.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.jack.common.constants.ErrorMessages.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxRepository outboxRepository; // Outbox repository for saving outbox events
    private final AuthServiceClient authServiceClient;
    private final UsersMapper usersMapper;
    private final RedisTemplate<String, WalletBalanceDto> redisTemplate;

    @Value("${app.wallet.cache-prefix}")
    private String cachePrefix;

    @Value("${app.wallet.routing-key.create}")
    private String routingKey;

    @Override
    public UserResponseDto register(UserRegistrationDto registrationDTO) {
        // Check if user already exists
        if (usersRepository.findByEmail(registrationDTO.getEmail()).isPresent()) {
            logger.error("User registration failed. User with email '{}' already exists", registrationDTO.getEmail());
            throw new RuntimeException(EMAIL_ALREADY_REGISTERED_BY_ANOTHER_USER);
        }

        // Encode the password before saving
        String encodedPassword = passwordEncoder.encode(registrationDTO.getPassword());

        // Create a new user
        Users newUser = Users.builder()
                .name(registrationDTO.getName())
                .email(registrationDTO.getEmail())
                .password(encodedPassword)
                .build();

        Users savedUser = usersRepository.save(newUser);

        // Programmatically log the user in by calling auth-service
        AuthRequestDTO authRequest = new AuthRequestDTO(savedUser.getEmail(), registrationDTO.getPassword());
        AuthResponseDTO authResponse = authServiceClient.login(authRequest);

        // Save a wallet creation message in the Outbox (No direct interaction with Outbox Service)
        double initialBalance = 1000.00;
        Outbox outboxEvent = Outbox.builder()
                .aggregateId(savedUser.getId())  // User ID
                .aggregateType("User")  // The type of entity related to the outbox
                .payload("{ \"userId\": " + savedUser.getId() + ", \"initialBalance\": " + initialBalance + " }")
                .routingKey(routingKey) // Use the routing key defined for wallet creation
                .status(EventStatus.PENDING)  // Set initial status as PENDING
                .createdAt(LocalDateTime.now())  // Event creation time
                .build();

        // Save the outbox event in the local outbox table
        outboxRepository.save(outboxEvent);  // The separate Outbox Service will pick this up

        // Return user details and JWT token
        return UserResponseDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .token(authResponse.getToken())  // Include JWT token in the response
                .build();
    }

    @Override
    public Optional<Users> updateUser(Long id, Users users) {
        logger.info("Attempting to update user with ID: {}", id);
        Users existingUser = findUserById(id);

        if (usersRepository.findByEmail(users.getEmail()).filter(user -> !user.getId().equals(id)).isPresent()) {
            logger.error("Email {} is already registered by another user.", users.getEmail());
            throw new CustomErrorException(
                    HttpStatus.CONFLICT,
                    EMAIL_ALREADY_REGISTERED_BY_ANOTHER_USER,
                    PUT_USER_API_PATH + id
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
                    HttpStatus.UNAUTHORIZED,
                    INVALID_EMAIL_OR_PASSWORD,
                    POST_LOGIN_API_PATH
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
        Users user = findUserByEmail(email);  // Find the user by email
        return passwordEncoder.matches(rawPassword, user.getPassword());  // Verify the password
    }

    @Override
    public UsersDTO getUserWithBalance(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new CustomErrorException(HttpStatus.NOT_FOUND, USER_NOT_FOUND, GET_USER_API_PATH + userId));

        UsersDTO usersDTO = usersMapper.toDto(user);

        // Fetch balance from Redis (updated by wallet-service)
        String cacheKey = cachePrefix + userId;
        WalletBalanceDto cachedBalance = redisTemplate.opsForValue().get(cacheKey);

        if (cachedBalance != null) {
            logger.info("Returning balance from Redis for user ID: {}", userId);
            usersDTO.setUsdBalance(cachedBalance.getUsdBalance());
            usersDTO.setBtcBalance(cachedBalance.getBtcBalance());
        } else {
            logger.warn("Balance not found in Redis for user ID: {}", userId);
            usersDTO.setUsdBalance(0.0);  // Default balance
            usersDTO.setBtcBalance(0.0);
        }

        return usersDTO;
    }

    private Users findUserById(Long id) {
        return usersRepository.findById(id).orElseThrow(() -> {
            logger.error("User with ID: {} not found.", id);
            return new CustomErrorException(
                    HttpStatus.NOT_FOUND,
                    USER_NOT_FOUND,
                    GET_USER_API_PATH + id
            );
        });
    }

    private Users findUserByEmail(String email) {
        return usersRepository.findByEmail(email).orElseThrow(() -> {
            logger.error("Invalid email or password for email: {}", email);
            return new CustomErrorException(
                    HttpStatus.UNAUTHORIZED,
                    INVALID_EMAIL_OR_PASSWORD,
                    POST_LOGIN_API_PATH
            );
        });
    }
}
