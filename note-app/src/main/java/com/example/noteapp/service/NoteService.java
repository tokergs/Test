package com.example.noteapp.service;

import com.example.noteapp.dto.NoteRequestDto;
import com.example.noteapp.dto.NoteResponseDto;
import com.example.noteapp.model.Note;
import com.example.noteapp.model.User;
import com.example.noteapp.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public List<NoteResponseDto> getUserNotes(User user) {
        return noteRepository.findAllByCreator(user)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public NoteResponseDto create(NoteRequestDto request, User user) {

        Note note = new Note(
                request.getTitle(),
                request.getContent(),
                user
        );

        return toDto(noteRepository.save(note));
    }

    private NoteResponseDto toDto(Note note) {
        NoteResponseDto dto = new NoteResponseDto();
        dto.id = note.getId().intValue();
        dto.title = note.getTitle();
        dto.content = note.getContent();
        dto.createdAt = note.getCreatedAt().toString();
        return dto;
    }
}
