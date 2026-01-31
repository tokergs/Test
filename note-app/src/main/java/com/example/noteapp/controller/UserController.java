package com.example.noteapp.controller;

import com.example.noteapp.dto.UserRequestDto;
import com.example.noteapp.dto.UserResponseDto;
import com.example.noteapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 1. Регистрация - публичный доступ
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRequestDto request) {
        try {
            UserResponseDto user = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // 2. Получить свой профиль - только для авторизованных
    @SecurityRequirement(name = "bearerAuth") // требует токен
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        logger.info("User accessing profile: {}", username);

        UserResponseDto user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    // 3. Получить пользователя по ID - с проверкой доступа
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        UserResponseDto user = userService.getUserByIdWithAccessCheck(id, currentUsername);
        return ResponseEntity.ok(user);
    }

    // 4. Получить ВСЕХ пользователей - ТОЛЬКО для ADMIN
    @SecurityRequirement(name = "bearerAuth") // требует токен
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // 5. Обновить свой профиль
    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateCurrentUser(
            @Valid @RequestBody UserRequestDto request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        UserResponseDto updatedUser = userService.updateUserByUsername(username, request);
        return ResponseEntity.ok(updatedUser);
    }

    // Вспомогательные классы для ответов
    public record ErrorResponse(String message) {
    }
}