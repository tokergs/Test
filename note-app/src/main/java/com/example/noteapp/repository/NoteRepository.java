package com.example.noteapp.repository;

import com.example.noteapp.model.Note;
import com.example.noteapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findAllByCreator(User creator);
}
