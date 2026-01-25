package com.example.noteapp.dto;

public record AuthRequestDto(
        String username,
        String password
) {}