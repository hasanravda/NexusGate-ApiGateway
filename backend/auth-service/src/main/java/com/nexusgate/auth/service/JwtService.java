package com.nexusgate.auth.service;

import com.nexusgate.auth.dto.JwtValidationResponse;
import com.nexusgate.auth.dto.TokenIntrospectionResponse;
import com.nexusgate.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    public JwtService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    public JwtValidationResponse validateToken(String token) {
        try {
            if (jwtTokenProvider.isTokenValid(token)) {
                String subject = jwtTokenProvider.getSubject(token);
                String role = jwtTokenProvider.getRole(token);
                Date expiresAt = jwtTokenProvider.getExpirationDate(token);
                
                return new JwtValidationResponse(
                        true,
                        subject,
                        role,
                        expiresAt.getTime() / 1000
                );
            } else {
                return new JwtValidationResponse(false, null, null, null);
            }
        } catch (Exception e) {
            return new JwtValidationResponse(false, null, null, null);
        }
    }
    
    public String refreshToken(String token) {
        if (!jwtTokenProvider.isTokenValid(token)) {
            throw new RuntimeException("Invalid token");
        }
        
        if (jwtTokenProvider.isTokenExpired(token)) {
            throw new RuntimeException("Token expired");
        }
        
        String subject = jwtTokenProvider.getSubject(token);
        String role = jwtTokenProvider.getRole(token);
        
        return jwtTokenProvider.generateAccessToken(subject, role);
    }
    
    public TokenIntrospectionResponse introspectToken(String token) {
        try {
            boolean isValid = jwtTokenProvider.isTokenValid(token);
            
            if (!isValid) {
                return new TokenIntrospectionResponse(false, null, null, null, null);
            }
            
            Claims claims = jwtTokenProvider.validateAndParseClaims(token);
            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
            Date issuedAt = claims.getIssuedAt();
            Date expiresAt = claims.getExpiration();
            boolean isExpired = jwtTokenProvider.isTokenExpired(token);
            
            return new TokenIntrospectionResponse(
                    !isExpired,
                    subject,
                    role,
                    issuedAt != null ? issuedAt.getTime() / 1000 : null,
                    expiresAt != null ? expiresAt.getTime() / 1000 : null
            );
        } catch (Exception e) {
            return new TokenIntrospectionResponse(false, null, null, null, null);
        }
    }
}
