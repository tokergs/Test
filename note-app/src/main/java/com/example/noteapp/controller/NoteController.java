package com.example.noteapp.controller;

import com.example.noteapp.dto.NoteRequestDto;
import com.example.noteapp.dto.NoteResponseDto;
import com.example.noteapp.model.Note;
import com.example.noteapp.model.User;
import com.example.noteapp.service.NoteService;
import com.example.noteapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);
    private final NoteService noteService;
    private final UserRepository userRepository;

    public NoteController(NoteService noteService,
                          UserRepository userRepository) {
        this.noteService = noteService;
        this.userRepository = userRepository;
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public List<NoteResponseDto> listNotes() {
        User currentUser = getCurrentUser();

        // ДОБАВЬ ЛОГИРОВАНИЕ:
        logger.info("User {} is accessing their notes", currentUser.getUsername());
        return noteService.getUserNotes(getCurrentUser());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponseDto createNote(@Valid @RequestBody NoteRequestDto request) {
        return noteService.create(request, getCurrentUser());
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow();
    }

    @DeleteMapping("/{id}")
    public String deleteNote(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        noteService.delete(id, currentUser);
        return "Note deleted successfully";
    }

}
