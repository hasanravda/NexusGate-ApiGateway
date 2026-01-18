package com.nexusgate.config_service.service;

import com.nexusgate.config_service.dto.*;
import com.nexusgate.config_service.model.User;
import com.nexusgate.config_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * Get all users
     */
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Register a new user (Demo - No actual password hashing)
     */
    @Transactional
    public UserDto register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user (Demo: storing plain password - NOT RECOMMENDED IN PRODUCTION)
        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())  // Demo: plain text (should be hashed in production)
                .fullName(request.getFullName())
                .role(request.getRole() != null ? request.getRole() : "VIEWER")
                .isActive(true)
                .build();
        
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    /**
     * Sign in user (Demo - Simple password check)
     */
    public SignInResponse signIn(SignInRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        // Demo: Simple password check (should use password encoder in production)
        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        
        if (!user.getIsActive()) {
            throw new RuntimeException("Account is inactive");
        }
        
        // Demo: Return a simple token (should use JWT in production)
        String demoToken = "demo_token_" + user.getId() + "_" + System.currentTimeMillis();
        
        return SignInResponse.builder()
                .token(demoToken)
                .user(convertToDto(user))
                .message("Sign in successful")
                .build();
    }
    
    /**
     * Get current user by email (Demo - simulating /me endpoint)
     */
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDto(user);
    }
    
    /**
     * Get user by ID
     */
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDto(user);
    }
    
    /**
     * Convert User entity to UserDto
     */
    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
