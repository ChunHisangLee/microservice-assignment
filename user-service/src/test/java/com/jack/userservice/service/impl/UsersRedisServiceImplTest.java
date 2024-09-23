package com.jack.userservice.service.impl;

import com.jack.common.constants.UsersConstants;
import com.jack.userservice.dto.UsersDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersRedisServiceImplTest {

    @Mock
    private RedisTemplate<String, UsersDto> redisTemplate;

    @Mock
    private ValueOperations<String, UsersDto> valueOperations;

    @InjectMocks
    private UsersRedisServiceImpl usersRedisService;

    private UsersDto sampleUser;

    @BeforeEach
    void setUp() {
        // Initialize a sample UsersDto object
        sampleUser = new UsersDto();
        sampleUser.setId(1L);
        sampleUser.setName("John Doe");
        sampleUser.setEmail("john.doe@example.com");

        // Leniently mock the RedisTemplate to return the mocked ValueOperations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void saveUserToRedis_ShouldSaveUser() {
        // Act
        usersRedisService.saveUserToRedis(sampleUser);

        // Assert
        String expectedKey = UsersConstants.USER_CACHE_PREFIX + sampleUser.getId();
        verify(valueOperations, times(1)).set(expectedKey, sampleUser);
    }

    @Test
    void getUserFromRedis_ShouldReturnUser_WhenUserExists() {
        // Arrange
        String expectedKey = UsersConstants.USER_CACHE_PREFIX + sampleUser.getId();
        when(valueOperations.get(expectedKey)).thenReturn(sampleUser);

        // Act
        UsersDto retrievedUser = usersRedisService.getUserFromRedis(sampleUser.getId());

        // Assert
        assertNotNull(retrievedUser, "Retrieved user should not be null");
        assertEquals(sampleUser.getId(), retrievedUser.getId(), "User ID should match");
        assertEquals(sampleUser.getName(), retrievedUser.getName(), "User name should match");
        assertEquals(sampleUser.getEmail(), retrievedUser.getEmail(), "User email should match");
        verify(valueOperations, times(1)).get(expectedKey);
    }

    @Test
    void getUserFromRedis_ShouldReturnNull_WhenUserDoesNotExist() {
        // Arrange
        String expectedKey = UsersConstants.USER_CACHE_PREFIX + sampleUser.getId();
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // Act
        UsersDto retrievedUser = usersRedisService.getUserFromRedis(sampleUser.getId());

        // Assert
        assertNull(retrievedUser, "Retrieved user should be null when not present in Redis");
        verify(valueOperations, times(1)).get(expectedKey);
    }

    @Test
    void deleteUserFromRedis_ShouldDeleteUser() {
        // Act
        usersRedisService.deleteUserFromRedis(sampleUser.getId());

        // Assert
        String expectedKey = UsersConstants.USER_CACHE_PREFIX + sampleUser.getId();
        verify(redisTemplate, times(1)).delete(expectedKey);
    }

    @Test
    void saveUserToRedis_ShouldHandleNullUser() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> usersRedisService.saveUserToRedis(null), "Saving a null user should throw NullPointerException");

        // Alternatively, if your implementation handles nulls gracefully, adjust accordingly
    }

    @Test
    void getUserFromRedis_ShouldHandleNullUserId() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> usersRedisService.getUserFromRedis(null), "Getting a user with null ID should throw NullPointerException");
    }

    @Test
    void deleteUserFromRedis_ShouldHandleNullUserId() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> usersRedisService.deleteUserFromRedis(null), "Deleting a user with null ID should throw NullPointerException");
    }

}
