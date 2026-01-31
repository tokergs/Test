package com.example.noteapp.unit;

import com.example.noteapp.dto.UserRequestDto;
import com.example.noteapp.dto.UserResponseDto;
import com.example.noteapp.model.User;
import com.example.noteapp.repository.UserRepository;
import com.example.noteapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_validInput_shouldSaveUserWithEncodedPassword() {
        // Arrange
        UserRequestDto request = new UserRequestDto();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");

        String encodedPassword = "encoded_hashed_password";

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1); // Устанавливаем ID как будто после сохранения
                    return user;
                });

        // Act
        UserResponseDto result = userService.createUser(request);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.username);
        assertEquals("test@example.com", result.email);
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));

        // Проверяем что сохраняется правильный User
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals(encodedPassword, savedUser.getPassword());
        assertEquals("USER", savedUser.getRole());
    }

    @Test
    void createUser_duplicateUsername_shouldThrowException() {
        // Arrange
        UserRequestDto request = new UserRequestDto();
        request.setUsername("existinguser");
        request.setEmail("existing@example.com");
        request.setPassword("Password123!");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(request));

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void createUser_invalidPassword_shouldThrowException() {
        // Arrange
        UserRequestDto request = new UserRequestDto();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("123");  // Слишком короткий (< 8 символов)

        when(userRepository.existsByUsername("testuser")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(request));

        assertEquals("Password must be at least 8 characters long", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void getUserByUsername_userExists_shouldReturnUser() {
        // Arrange
        String username = "testuser";
        User user = new User();
        user.setId(1);
        user.setUsername(username);
        user.setEmail("test@example.com");
        user.setPassword("encodedPass");
        user.setRole("USER");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserResponseDto result = userService.getUserByUsername(username);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.username);
        assertEquals("test@example.com", result.email);
        assertEquals("USER", result.role);
        verify(userRepository).findByUsername(username);
    }

    @Test
    void getUserByUsername_userNotFound_shouldThrowException() {
        // Arrange
        String username = "nonexistent";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.getUserByUsername(username));

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository).findByUsername(username);
    }

    @Test
    void updateUserByUsername_validInput_shouldUpdateUser() {
        // Arrange
        String username = "testuser";
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setUsername(username);
        existingUser.setEmail("old@example.com");
        existingUser.setPassword("oldEncodedPass");
        existingUser.setRole("USER");

        UserRequestDto updateRequest = new UserRequestDto();
        updateRequest.setUsername("newname"); // Меняем username
        updateRequest.setEmail("new@example.com");
        updateRequest.setPassword("NewPassword123!");

        String newEncodedPassword = "newEncodedPass";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername("newname")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn(newEncodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        UserResponseDto result = userService.updateUserByUsername(username, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals("newname", result.username);
        assertEquals("new@example.com", result.email);

        // Проверяем, что пароль обновился
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals(newEncodedPassword, updatedUser.getPassword());
    }

    @Test
    void updateUserByUsername_duplicateNewUsername_shouldThrowException() {
        // Arrange
        String username = "testuser";
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setUsername(username);
        existingUser.setEmail("test@example.com");
        existingUser.setRole("USER");

        UserRequestDto updateRequest = new UserRequestDto();
        updateRequest.setUsername("takenusername"); // Этот username уже занят

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername("takenusername")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserByUsername(username, updateRequest));

        assertEquals("Username already taken", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserByUsername_shortPassword_shouldThrowException() {
        // Arrange
        String username = "testuser";
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setUsername(username);
        existingUser.setEmail("test@example.com");
        existingUser.setRole("USER");

        UserRequestDto updateRequest = new UserRequestDto();
        updateRequest.setPassword("123"); // Слишком короткий пароль

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserByUsername(username, updateRequest));

        assertEquals("Password must be at least 8 characters long", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}