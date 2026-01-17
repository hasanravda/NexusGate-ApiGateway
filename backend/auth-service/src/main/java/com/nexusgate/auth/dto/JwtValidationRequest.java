package com.nexusgate.auth.dto;

public class JwtValidationRequest {
    
    private String token;
    
    public JwtValidationRequest() {
    }
    
    public JwtValidationRequest(String token) {
        this.token = token;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}
