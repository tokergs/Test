package com.example.noteapp.service;

import com.example.noteapp.dto.UserRequestDto;
import com.example.noteapp.dto.UserResponseDto;
import com.example.noteapp.model.User;
import com.example.noteapp.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. Получить всех пользователей (только для ADMIN)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    // 2. Получить пользователя по username
    public UserResponseDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return convertToResponseDto(user);
    }

    // 3. Получить пользователя по ID с проверкой доступа
    public UserResponseDto getUserByIdWithAccessCheck(Integer id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        // Проверка: пользователь может получить только СВОЙ профиль, если он не ADMIN
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

        if (!currentUser.getRole().equals("ADMIN") && !user.getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only access your own profile");
        }

        return convertToResponseDto(user);
    }

    // 4. Создать пользователя (регистрация)
    public UserResponseDto createUser(UserRequestDto userRequest) {
        // Проверка на существующего пользователя
        if (userRepository.existsByUsername(userRequest.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Password policy: минимум 8 символов
        if (userRequest.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        User user = convertToEntity(userRequest);
        User saved = userRepository.save(user);
        return convertToResponseDto(saved);
    }

    // 5. Обновить пользователя по username
    public UserResponseDto updateUserByUsername(String username, UserRequestDto userRequest) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (userRequest.getUsername() != null &&
                !userRequest.getUsername().isEmpty() &&
                !userRequest.getUsername().equals(user.getUsername())) {

            // Проверяем, не занят ли новый username другим пользователем
            if (userRepository.existsByUsername(userRequest.getUsername())) {
                throw new IllegalArgumentException("Username already taken");
            }

            user.setUsername(userRequest.getUsername());
        }

        // Обновляем только разрешенные поля
        if (userRequest.getEmail() != null) {
            user.setEmail(userRequest.getEmail());
        }

        // Если указан новый пароль
        if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
            if (userRequest.getPassword().length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters long");
            }
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        }

        User updated = userRepository.save(user);
        return convertToResponseDto(updated);
    }

    // Вспомогательные методы
    private User convertToEntity(UserRequestDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER"); // По умолчанию USER, ADMIN создается через БД
        return user;
    }

    private UserResponseDto convertToResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.role = user.getRole();
        return dto;
    }
}