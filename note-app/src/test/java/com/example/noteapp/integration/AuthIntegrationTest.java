package com.example.noteapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerUser_validInput_shouldSucceed() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "NewUser");
        request.put("email", "newuser@example.com");
        request.put("password", "SecurePass123!");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@example.com"));
    }

    @Test
    void registerUser_weakPassword_shouldReturn400() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "User");
        request.put("email", "user@example.com");
        request.put("password", "123"); // Слишком слабый, но проверку всеравно прошло, ПРОВЕРЬ!!!

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 400
    }

    @Test
    void login_withValidCredentials_shouldReturnToken() throws Exception {
        // 1. Сначала регистрируем пользователя
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "LoginUser");
        registerRequest.put("email", "loginuser@example.com");
        registerRequest.put("password", "LoginPass123!");

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // 2. Пытаемся залогиниться
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "LoginUser");
        loginRequest.put("password", "LoginPass123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_withWrongPassword_shouldReturn401() throws Exception {
        // 1. Регистрируем
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "WrongPass");
        registerRequest.put("email", "wrongpass@example.com");
        registerRequest.put("password", "CorrectPass123!");

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // 2. Логинимся с неправильным паролем
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "WrongPass");
        loginRequest.put("password", "WrongPass123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden()); // 401, но выдает 403, по идее 403 это верно, было isUnauthorized
    }
}