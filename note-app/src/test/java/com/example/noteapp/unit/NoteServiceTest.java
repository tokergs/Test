package com.example.noteapp.unit;

import com.example.noteapp.dto.NoteRequestDto;
import com.example.noteapp.dto.NoteResponseDto;
import com.example.noteapp.model.Note;
import com.example.noteapp.model.User;
import com.example.noteapp.repository.NoteRepository;
import com.example.noteapp.service.NoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private User mockUser;

    @InjectMocks
    private NoteService noteService;

    @Test
    void create_validInput_shouldSaveNote() {
        // Arrange
        NoteRequestDto request = new NoteRequestDto();
        request.setTitle("Test Note");
        request.setContent("Test Content");

        when(mockUser.getId()).thenReturn(1);
        when(mockUser.getUsername()).thenReturn("user1");

        Note savedNote = new Note("Test Note", "Test Content", mockUser);
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);

        // Act
        NoteResponseDto result = noteService.create(request, mockUser);

        // Assert
        assertNotNull(result);
        assertEquals("Test Note", result.title);
        assertEquals("Test Content", result.content);
        verify(noteRepository).save(any(Note.class));

        // Проверяем, что Note создается с правильными параметрами
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        Note capturedNote = noteCaptor.getValue();
        assertEquals("Test Note", capturedNote.getTitle());
        assertEquals("Test Content", capturedNote.getContent());
        assertEquals(mockUser, capturedNote.getCreator());
    }

    @Test
    void getUserNotes_shouldReturnOnlyUsersNotes() {
        // Arrange
        User mockUser2 = mock(User.class);
        when(mockUser2.getId()).thenReturn(2);
        when(mockUser2.getUsername()).thenReturn("user2");

        Note note1 = new Note("Note 1", "Content 1", mockUser);
        Note note2 = new Note("Note 2", "Content 2", mockUser);

        List<Note> userNotes = Arrays.asList(note1, note2);
        when(noteRepository.findAllByCreator(mockUser))
                .thenReturn(userNotes);

        // Act
        List<NoteResponseDto> result = noteService.getUserNotes(mockUser);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Note 1", result.get(0).title);
        assertEquals("Note 2", result.get(1).title);
        verify(noteRepository).findAllByCreator(mockUser);
    }

    @Test
    void delete_noteExistsAndUserOwnsIt_shouldDelete() {
        // Arrange
        Long noteId = 1L;

        Note note = new Note();
        note.setTitle("Test");
        note.setContent("Content");
        note.setCreator(mockUser);

        when(mockUser.getId()).thenReturn(1);
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));

        // Act & Assert - теперь void, не возвращает boolean
        assertDoesNotThrow(() -> noteService.delete(noteId, mockUser));

        // Assert
        verify(noteRepository).delete(note);
    }

    @Test
    void delete_noteDoesNotExist_shouldThrowException() {
        // Arrange
        Long noteId = 1L;

        when(mockUser.getId()).thenReturn(1);
        when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> noteService.delete(noteId, mockUser));

        assertTrue(exception.getMessage().contains("Note not found"));
        verify(noteRepository, never()).delete(any(Note.class));
    }

    @Test
    void delete_noteExistsButUserDoesNotOwnIt_shouldThrowException() {
        // Arrange
        Long noteId = 1L;

        User otherUser = mock(User.class);
        when(otherUser.getId()).thenReturn(2);

        Note note = new Note();
        note.setTitle("Test");
        note.setContent("Content");
        note.setCreator(otherUser);

        when(mockUser.getId()).thenReturn(1);
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> noteService.delete(noteId, mockUser));

        assertTrue(exception.getMessage().contains("Access denied"));
        verify(noteRepository, never()).delete(any(Note.class));
    }
}