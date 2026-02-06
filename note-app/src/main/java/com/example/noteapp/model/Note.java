package com.example.noteapp.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notes")
public class Note {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String content;

    private Instant createdAt = Instant.now();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // привязка к пользователю через внешний ключ user_id
    private User creator;

    public Note() {
        // JPA constructor
    }

    public Note(String title, String content, User creator) {
        this.title = title;
        this.content = content;
        this.creator = creator;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }
}
