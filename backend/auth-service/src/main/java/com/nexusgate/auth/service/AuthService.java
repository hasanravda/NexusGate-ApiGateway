package com.nexusgate.auth.service;

import com.nexusgate.auth.dto.LoginResponse;
import com.nexusgate.auth.dto.UserInfoResponse;
import com.nexusgate.auth.model.User;
import com.nexusgate.auth.repository.UserRepository;
import com.nexusgate.auth.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    public LoginResponse authenticate(String email, String password) {
        User user = userRepository.findByEmailAndIsActive(email, true)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        String token = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());
        Date expiresAt = jwtTokenProvider.getExpirationDate(token);
        
        return new LoginResponse(token, expiresAt.getTime() / 1000);
    }
    
    public UserInfoResponse getUserInfo(String token) {
        String email = jwtTokenProvider.getSubject(token);
        String role = jwtTokenProvider.getRole(token);
        
        return new UserInfoResponse(email, role);
    }
}
