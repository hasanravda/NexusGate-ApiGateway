package com.nexusgate.auth.controller;

import com.nexusgate.auth.dto.*;
import com.nexusgate.auth.security.JwtTokenProvider;
import com.nexusgate.auth.service.AuthService;
import com.nexusgate.auth.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final JwtService jwtService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    
    public AuthController(JwtService jwtService, JwtTokenProvider jwtTokenProvider, AuthService authService) {
        this.jwtService = jwtService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            if (request.getEmail() == null || request.getPassword() == null) {
                return ResponseEntity.badRequest().build();
            }
            
            LoginResponse response = authService.authenticate(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String token = authHeader.substring(7);
            UserInfoResponse response = authService.getUserInfo(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    
    @PostMapping("/validate")
    public ResponseEntity<JwtValidationResponse> validateToken(@RequestBody JwtValidationRequest request) {
        if (request.getToken() == null || request.getToken().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new JwtValidationResponse(false, null, null, null));
        }
        
        JwtValidationResponse response = jwtService.validateToken(request.getToken());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            if (request.getToken() == null || request.getToken().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            String newToken = jwtService.refreshToken(request.getToken());
            Long expiresAt = jwtTokenProvider.getExpirationDate(newToken).getTime() / 1000;
            
            RefreshTokenResponse response = new RefreshTokenResponse(newToken, expiresAt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    
    @PostMapping("/introspect")
    public ResponseEntity<TokenIntrospectionResponse> introspectToken(@RequestBody JwtValidationRequest request) {
        if (request.getToken() == null || request.getToken().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new TokenIntrospectionResponse(false, null, null, null, null));
        }
        
        TokenIntrospectionResponse response = jwtService.introspectToken(request.getToken());
        return ResponseEntity.ok(response);
    }
}
