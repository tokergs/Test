package com.example.noteapp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, String> registerRequest = Map.of(
                "username", "securityUser",
                "email", "security@example.com",
                "password", "SecurePass123!"
        );

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "securityUser",
                                "password", "SecurePass123!"
                        ))))
                .andReturn();

        authToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    // 1. Обязательный тест: Security Headers
    @Test
    void securityHeaders_shouldBePresent() throws Exception {
        mockMvc.perform(get("/notes")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("Referrer-Policy"));
    }

    // 2. Stateless JWT аутентификация
    @Test
    void jwtAuthentication_shouldBeStateless() throws Exception {
        mockMvc.perform(get("/notes")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    // 3. Public endpoints доступны
    @Test
    void registrationAndLogin_shouldBePublic() throws Exception {
        // Регистрация
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "publicUser",
                                "email", "public@example.com",
                                "password", "PublicPass123!"
                        ))))
                .andExpect(status().is2xxSuccessful());

        // Логин
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "publicUser",
                                "password", "PublicPass123!"
                        ))))
                .andExpect(status().isOk());
    }
}