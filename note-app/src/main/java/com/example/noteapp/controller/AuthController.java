package com.example.noteapp.controller;

import com.example.noteapp.dto.AuthRequestDto;
import com.example.noteapp.dto.AuthResponseDto;
import com.example.noteapp.dto.RefreshTokenRequestDto;
import com.example.noteapp.model.User;
import com.example.noteapp.service.CustomUserDetailsService;
import com.example.noteapp.service.JwtService;
import com.example.noteapp.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authenticationManager,
                          CustomUserDetailsService userDetailsService,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public AuthResponseDto login(@RequestBody AuthRequestDto request) {
        logger.info("Login attempt for user: {}", request.username());

        try {
            // Аутентификация
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            // Получаем пользователя через UserDetailsService
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());

            // Приводим к User (если CustomUserDetailsService возвращает User)
            User user = (User) userDetails;

            // Генерируем access token
            String accessToken = jwtService.generateToken(request.username());

            // Генерируем refresh token
            var refreshToken = refreshTokenService.createRefreshToken(user);

            logger.info("Successful login for user: {}", request.username());

            return new AuthResponseDto(accessToken, refreshToken.getToken());

        } catch (Exception e) {
            logger.warn("Failed login attempt for user: {} - {}", request.username(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/refresh")
    public AuthResponseDto refreshToken(@RequestBody RefreshTokenRequestDto request) {
        logger.info("Refresh token attempt");

        try {
            // Проверяем refresh token
            var refreshToken = refreshTokenService.verifyRefreshToken(request.refreshToken());

            // Отзываем старый refresh token (ROTATION!)
            refreshTokenService.revokeRefreshToken(request.refreshToken());

            // Получаем пользователя из refresh token
            User user = refreshToken.getUser();

            // Генерируем новый access token
            String newAccessToken = jwtService.generateToken(user.getUsername());

            // Генерируем НОВЫЙ refresh token (ROTATION!)
            var newRefreshToken = refreshTokenService.createRefreshToken(user);

            logger.info("Token refreshed successfully for user: {}", user.getUsername());

            return new AuthResponseDto(newAccessToken, newRefreshToken.getToken());

        } catch (Exception e) {
            logger.warn("Failed refresh token attempt: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/logout")
    public String logout(@RequestBody RefreshTokenRequestDto request) {
        logger.info("Logout request");

        // Отзываем refresh token (на клиенте нужно удалить access token)
        refreshTokenService.revokeRefreshToken(request.refreshToken());

        logger.info("Refresh token revoked successfully");
        return "Logged out successfully";
    }
}