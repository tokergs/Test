package com.example.noteapp.integration;

import com.example.noteapp.repository.NoteRepository;
import com.example.noteapp.repository.RefreshTokenRepository;
import com.example.noteapp.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
@AutoConfigureMockMvc
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

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

    @AfterEach
    void tearDown(){
        refreshTokenRepository.deleteAll();
        noteRepository.deleteAll();
        userRepository.deleteAll();
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
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("First note"));
    }

    @Test
    void createNote_withInvalidData_shouldReturn400() throws Exception {
        NotePayload payload = new NotePayload("", "Content");

        mockMvc.perform(post("/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createNote_withoutAuthentication_shouldReturn401() throws Exception {
        NotePayload payload = new NotePayload("Test", "Content");

        // Делаем запрос БЕЗ токена авторизации
        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden()); // Теперь 401 будет корректным
    }

    @Test
    void deleteNote_thatExists_shouldReturn200() throws Exception {
        // 1. Создаём заметку
        NotePayload payload = new NotePayload("To Delete", "Content");

        String response = mockMvc.perform(post("/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn().getResponse().getContentAsString();

        String noteId = objectMapper.readTree(response).get("id").asText();

        // 2. Удаляем её
        mockMvc.perform(delete("/notes/" + noteId) // ДОБАВЬТЕ СЛЕШ!
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // 3. Проверяем, что её больше нет
        mockMvc.perform(get("/notes")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteNote_thatDoesNotExist_shouldReturn404() throws Exception {
        // Пытаемся удалить несуществующую заметку
        mockMvc.perform(delete("/notes/999999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    record NotePayload(String title, String content) {}
}