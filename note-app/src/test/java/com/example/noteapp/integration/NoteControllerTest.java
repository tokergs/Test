package com.example.noteapp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken; // Для хранения JWT токена

    @BeforeEach
    void setUp() throws Exception {
        // 1. Регистрируем пользователя
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "loginUser");
        registerRequest.put("email", "loginuser@example.com");
        registerRequest.put("password", "LoginPass123!");

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // 2. Логинимся и получаем токен (предполагаем JWT)
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "loginUser");
        loginRequest.put("password", "LoginPass123!");

        MvcResult result = mockMvc.perform(post("/auth/login") // или ваш эндпоинт аутентификации
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 3. Извлекаем токен из ответа
        // Предположим, что токен возвращается в поле "token"
        String response = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        authToken = jsonNode.get("token").asText();
    }

    @Test
    void createAndListNotes() throws Exception {
        NotePayload payload = new NotePayload("First note", "Sample content");

        mockMvc.perform(post("/notes")
                        .header("Authorization", "Bearer " + authToken) // Используем реальный токен
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("First note"));

        mockMvc.perform(get("/notes")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("First note"));
    }

    @Test
    void createNote_withEmptyTitle_shouldReturn400() throws Exception {
        String jsonEmpty = "{\"title\": \"\", \"content\": \"Content\"}";

        mockMvc.perform(post("/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEmpty))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createNote_withoutAuthentication_shouldReturn4xx() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("title", "Test");
        payload.put("content", "Content");

        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deleteNote_thatExists_shouldReturn200() throws Exception {
        // 1. Создаём заметку
        Map<String, String> payload = new HashMap<>();
        payload.put("title", "To Delete");
        payload.put("content", "Content");

        String response = mockMvc.perform(post("/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn().getResponse().getContentAsString();

        String noteId = objectMapper.readTree(response).get("id").asText();

        // 2. Удаляем её
        mockMvc.perform(delete("/notes/" + noteId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // 3. Проверяем список
        mockMvc.perform(get("/notes")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteNote_thatDoesNotExist_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/notes/999999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    record NotePayload(String title, String content) {}
}