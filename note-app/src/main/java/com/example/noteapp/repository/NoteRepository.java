package com.example.noteapp.repository;

import com.example.noteapp.model.Note;
import com.example.noteapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findAllByCreator(User creator);

    @Query(
            value = "SELECT * FROM notes WHERE user_id = ?1 AND title LIKE ?2",
            nativeQuery = true
    )
    List<Note> findByUserIdAndTitleLike(Long userId, String titlePattern);
}
