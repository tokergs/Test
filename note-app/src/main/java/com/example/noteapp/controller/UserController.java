package com.example.noteapp.controller;

import com.example.noteapp.dto.UserRequestDto;
import com.example.noteapp.dto.UserResponseDto;
import com.example.noteapp.model.User;
import com.example.noteapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequestDto request) {
        try {
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Username is required"));
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Password is required"));
            }

            UserResponseDto user = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    public record RegisterRequest(String username, String password) {
    }

    public record RegisterResponse(Long id, String username, String message) {
    }

    public record ErrorResponse(String message) {
    }
    /**
     * GET /users
     * Get all users
     * Returns: 200 OK with list of users
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}