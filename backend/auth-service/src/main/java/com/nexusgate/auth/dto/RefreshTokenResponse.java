package com.nexusgate.auth.dto;

public class RefreshTokenResponse {
    
    private String token;
    private Long expiresAt;
    
    public RefreshTokenResponse() {
    }
    
    public RefreshTokenResponse(String token, Long expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public Long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
