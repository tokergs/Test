package com.example.noteapp.controller;

import com.example.noteapp.dto.NoteRequestDto;
import com.example.noteapp.dto.NoteResponseDto;
import com.example.noteapp.model.Note;
import com.example.noteapp.model.User;
import com.example.noteapp.service.NoteService;
import com.example.noteapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);
    private final NoteService noteService; // ← ТОЛЬКО ОДНА ЗАВИСИМОСТЬ

    public NoteController(NoteService noteService) { // ← ТОЛЬКО ОДИН ПАРАМЕТР
        this.noteService = noteService;
    }

    @SecurityRequirement(name = "bearerAuth") // ← ОСТАЛОСЬ
    @GetMapping
    public ResponseEntity<List<NoteResponseDto>> listNotes(@AuthenticationPrincipal User currentUser) { // ← ДОБАВЛЕНО @AuthenticationPrincipal
        logger.info("User {} is accessing their notes", currentUser.getUsername());
        List<NoteResponseDto> notes = noteService.getUserNotes(currentUser); // ← ПЕРЕДАЁМ currentUser
        return ResponseEntity.ok(notes); // ← ОБЕРНУЛИ В ResponseEntity
    }

    @SecurityRequirement(name = "bearerAuth") // ← ДОБАВЛЕНО
    @PostMapping
    public ResponseEntity<NoteResponseDto> createNote( // ← ИЗМЕНИЛИ ВОЗВРАЩАЕМЫЙ ТИП
                                                       @Valid @RequestBody NoteRequestDto request,
                                                       @AuthenticationPrincipal User currentUser) { // ← ДОБАВЛЕНО @AuthenticationPrincipal

        logger.info("User {} is creating a note with title: {}",
                currentUser.getUsername(), request.getTitle()); // ← ДОБАВЛЕНО ЛОГИРОВАНИЕ
        NoteResponseDto note = noteService.create(request, currentUser); // ← ПЕРЕДАЁМ currentUser
        return ResponseEntity.status(HttpStatus.CREATED).body(note); // ← ОБЕРНУЛИ В ResponseEntity
    }

    @SecurityRequirement(name = "bearerAuth") // ← ДОБАВЛЕНО
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNote( // ← ИЗМЕНИЛИ ВОЗВРАЩАЕМЫЙ ТИП
                                              @PathVariable Long id,
                                              @AuthenticationPrincipal User currentUser) { // ← ДОБАВЛЕНО @AuthenticationPrincipal

        logger.info("User {} is deleting note with id: {}",
                currentUser.getUsername(), id); // ← ДОБАВЛЕНО ЛОГИРОВАНИЕ
        noteService.delete(id, currentUser); // ← ПЕРЕДАЁМ currentUser
        return ResponseEntity.ok("Note deleted successfully"); // ← ОБЕРНУЛИ В ResponseEntity
    }

    // УДАЛЕНЫ: метод getCurrentUser() и лишняя проверка в deleteNote
}
