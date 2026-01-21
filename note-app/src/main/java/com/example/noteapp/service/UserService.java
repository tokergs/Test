package com.example.noteapp.service;

import com.example.noteapp.dto.UserRequestDto;
import com.example.noteapp.dto.UserResponseDto;
import com.example.noteapp.model.User;
import com.example.noteapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public UserResponseDto getUserById(Integer id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return null;
        return convertToResponseDto(user);
    }

    public UserResponseDto createUser(UserRequestDto userRequest) {
        User user = convertToEntity(userRequest);
        User saved = userRepository.save(user);
        return convertToResponseDto(saved);
    }

    public UserResponseDto updateUser(Integer id, UserRequestDto userRequest) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return null;
        
        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());
        user.setPassword(userRequest.getPassword());
        
        User updated = userRepository.save(user);
        return convertToResponseDto(updated);
    }

    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }

    private User convertToEntity(UserRequestDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setRole("Role");
        return user;
    }

    private UserResponseDto convertToResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        return dto;
    }
}