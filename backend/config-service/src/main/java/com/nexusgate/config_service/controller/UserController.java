package com.nexusgate.config_service.controller;

import com.nexusgate.config_service.dto.*;
import com.nexusgate.config_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    
    /**
     * GET /api/users - List all users
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    /**
     * POST /api/users/register - Register new user (Demo)
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        UserDto user = userService.register(request);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }
    
    /**
     * POST /api/users/signin - Sign in user
     */
    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signIn(@Valid @RequestBody SignInRequest request) {
        SignInResponse response = userService.signIn(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/users/me - Get current authenticated user
     * Extracts user email from JWT token in Authorization header
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        UserDto user = userService.getCurrentUser(email);
        return ResponseEntity.ok(user);
    }
    
    /**
     * GET /api/users/{id} - Get user by ID
     * Regex pattern ensures only numeric IDs are matched
     */
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
}
