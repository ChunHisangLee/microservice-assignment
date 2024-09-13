package com.jack.userservice.service.impl;

import com.jack.userservice.client.AuthServiceClient;
import com.jack.userservice.dto.*;
import com.jack.userservice.entity.Users;
import com.jack.userservice.exception.CustomErrorException;
import com.jack.userservice.mapper.UsersMapper;
import com.jack.userservice.message.WalletCreationMessage;
import com.jack.userservice.repository.UsersRepository;
import com.jack.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.jack.userservice.constants.ErrorMessages.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;
    private final AuthServiceClient authServiceClient;
    private final UsersMapper usersMapper;
    private final RedisTemplate<String, WalletBalanceDTO> redisTemplate;

    @Value("${app.wallet.cache-prefix}")
    private String cachePrefix;

    @Value("${app.wallet.exchange}")
    private String exchange;

    @Value("${app.wallet.routing-key.create}")
    private String routingKey;

    @Override
    public UserResponseDTO register(UserRegistrationDTO registrationDTO) {
        // Check if user already exists
        if (usersRepository.findByEmail(registrationDTO.getEmail()).isPresent()) {
            logger.error("User registration failed. User with email '{}' already exists", registrationDTO.getEmail());
            throw new RuntimeException(EMAIL_ALREADY_REGISTERED_BY_ANOTHER_USER);
        }

        // Encode the password before saving
        String encodedPassword = passwordEncoder.encode(registrationDTO.getPassword());

        // Create a new user without encoding password, as auth-service will handle encryption
        Users newUser = Users.builder()
                .name(registrationDTO.getName())
                .email(registrationDTO.getEmail())
                .password(encodedPassword)
                .build();

        logger.info("AUTH_SERVICE_URL: {}", System.getenv("AUTH_SERVICE_URL"));
        Users savedUser = usersRepository.save(newUser);

        // Programmatically log the user in by calling auth-service
        AuthRequestDTO authRequest = new AuthRequestDTO(savedUser.getEmail(), registrationDTO.getPassword());
        AuthResponseDTO authResponse = authServiceClient.login(authRequest);  // Use Feign Client to call auth-service

        // Send a wallet creation message
        Double initialBalance = 1000.00;
        sendWalletCreationMessage(savedUser.getId(), initialBalance);  // Initial balance of 1000 USD
        // Return user details and JWT token
        return UserResponseDTO.builder()
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
        // Check if user exists
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new CustomErrorException(HttpStatus.NOT_FOUND, USER_NOT_FOUND, GET_USER_API_PATH + userId));

        UsersDTO usersDTO = usersMapper.toDto(user);

        // Fetch balance from Redis (updated by wallet-service)
        String cacheKey = cachePrefix + userId;
        WalletBalanceDTO cachedBalance = redisTemplate.opsForValue().get(cacheKey);

        if (cachedBalance != null) {
            logger.info("Returning balance from Redis for user ID: {}", userId);
            usersDTO.setUsdBalance(cachedBalance.getUsdBalance());
            usersDTO.setBtcBalance(cachedBalance.getBtcBalance());
        } else {
            logger.warn("Balance not found in Redis for user ID: {}", userId);
            usersDTO.setUsdBalance(null);
            usersDTO.setBtcBalance(null);
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

    private void sendWalletCreationMessage(Long userId, Double initialBalance) {
        WalletCreationMessage walletMessage = new WalletCreationMessage(userId, initialBalance);

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, walletMessage);
            logger.info("Wallet creation message sent for user ID: {}", userId);
        } catch (AmqpConnectException e) {
            logger.error("Connection to RabbitMQ failed: {}", e.getMessage());
        } catch (AmqpException e) {
            logger.error("Failed to send message to RabbitMQ: {}", e.getMessage());
            throw new CustomErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    FAILED_WALLET_CREATION,
                    POST_USER_API_PATH
            );
        }
    }
}
