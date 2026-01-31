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
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authTokenUser1;
    private String authTokenUser2;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Регистрируем и логиним первого пользователя
        authTokenUser1 = registerAndLogin("user1", "user1@test.com", "Pass123!");

        // 2. Регистрируем и логиним второго пользователя
        authTokenUser2 = registerAndLogin("user2", "user2@test.com", "Pass123!");
    }

    private String registerAndLogin(String username, String email, String password) throws Exception {
        // Регистрация
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", username);
        registerRequest.put("email", email);
        registerRequest.put("password", password);

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Логин
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    // ТЕСТ 1: Создание заметки с авторизацией
    @Test
    void createNote_shouldReturn201() throws Exception {
        Map<String, String> noteRequest = Map.of(
                "title", "Test Note",
                "content", "Test Content"
        );

        mockMvc.perform(post("/notes")
                        .header("Authorization", "Bearer " + authTokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Test Note"))
                .andExpect(jsonPath("$.content").value("Test Content"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    // ТЕСТ 2: Получение списка только своих заметок
    @Test
    void getNotes_shouldReturnOnlyOwnNotes() throws Exception {
        // User1 создает заметку
        Map<String, String> noteRequest1 = Map.of(
                "title", "User1 Note",
                "content", "Content 1"
        );

        mockMvc.perform(post("/notes")
                        .header("Authorization", "Bearer " + authTokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest1)))
                .andExpect(status().isCreated());

        // User2 создает свою заметку
        Map<String, String> noteRequest2 = Map.of(
                "title", "User2 Note",
                "content", "Content 2"
        );

        mockMvc.perform(post("/notes")
                        .header("Authorization", "Bearer " + authTokenUser2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest2)))
                .andExpect(status().isCreated());

        // User1 должен видеть только свою заметку
        mockMvc.perform(get("/notes")
                        .header("Authorization", "Bearer " + authTokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("User1 Note"));
    }

    // ТЕСТ 3: Удаление своей заметки
    @Test
    void deleteOwnNote_shouldReturnSuccess() throws Exception {
        // Создаем заметку
        Map<String, String> noteRequest = Map.of(
                "title", "Note to delete",
                "content", "Content"
        );

        String response = mockMvc.perform(post("/notes")
                        .header("Authorization", "Bearer " + authTokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest)))
                .andReturn().getResponse().getContentAsString();

        String noteId = objectMapper.readTree(response).get("id").asText();

        // Удаляем свою заметку
        mockMvc.perform(delete("/notes/" + noteId)
                        .header("Authorization", "Bearer " + authTokenUser1))
                .andExpect(status().isOk())
                .andExpect(content().string("Note deleted successfully"));

        // Проверяем, что список теперь пустой
        mockMvc.perform(get("/notes")
                        .header("Authorization", "Bearer " + authTokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ТЕСТ 4: Нельзя удалить чужую заметку
    @Test
    void deleteOtherUsersNote_shouldReturn403() throws Exception {
        // User1 создает заметку
        Map<String, String> noteRequest = Map.of(
                "title", "User1's Note",
                "content", "Content"
        );

        String response = mockMvc.perform(post("/notes")
                        .header("Authorization", "Bearer " + authTokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest)))
                .andReturn().getResponse().getContentAsString();

        String noteId = objectMapper.readTree(response).get("id").asText();

        // User2 пытается удалить заметку User1
        mockMvc.perform(delete("/notes/" + noteId)
                        .header("Authorization", "Bearer " + authTokenUser2))
                .andExpect(status().isForbidden());

        // User1 всё еще должен видеть свою заметку
        mockMvc.perform(get("/notes")
                        .header("Authorization", "Bearer " + authTokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("User1's Note"));
    }

    // ТЕСТ 5: Доступ без авторизации запрещен
    @Test
    void accessWithoutToken_shouldReturn401() throws Exception {
        Map<String, String> noteRequest = Map.of(
                "title", "Test",
                "content", "Content"
        );

        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest)))
                .andExpect(status().isUnauthorized());
    }
}