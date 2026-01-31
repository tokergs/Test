package com.example.noteapp.unit.validator;

import com.example.noteapp.model.Note;
import com.example.noteapp.model.User;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;

import static org.junit.jupiter.api.Assertions.*;

class NoteValidatorTest {

    private final Validator validator;

    public NoteValidatorTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void note_withValidData_shouldPassValidation() {
        User testUser = createTestUser();
        // Arrange
        Note note = new Note("Valid Title", "Valid content with more than 10 chars", testUser);

        // Act
        var violations = validator.validate(note);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    void note_withEmptyTitle_shouldFailValidation() {
        User testUser = createTestUser();
        // Arrange
        Note note = new Note("", "Content", testUser); // Пустой заголовок

        // Act
        var violations = validator.validate(note);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void note_withShortContent_shouldFailValidation() {
        User testUser = createTestUser();
        // Arrange
        Note note = new Note("Title", "short", testUser); // 5 символов < 10

        // Act
        var violations = validator.validate(note);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("content")));
    }

    @Test
    void note_withNullTitle_shouldFailValidation() {
        User testUser = createTestUser();
        // Arrange
        Note note = new Note(null, "Some content", testUser);

        // Act
        var violations = validator.validate(note);

        // Assert
        assertFalse(violations.isEmpty());
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password"); // если есть поле password
        // Установи все поля, которые есть в классе User
        return user;
    }

}