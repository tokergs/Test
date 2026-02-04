package com.example.noteapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NoteRequestDto {
    @NotBlank(message = "Title cannot be empty") //@NotNull не проверяет пустую строку, @NotBlank проверяет
    @Size(min = 1, max = 200)
    private String title;

    @Size(max = 5000)
    private String content;

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
}

