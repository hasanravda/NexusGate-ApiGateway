package com.nexusgate.auth.dto;

public class JwtValidationResponse {
    
    private boolean valid;
    private String subject;
    private String role;
    private Long expiresAt;
    
    public JwtValidationResponse() {
    }
    
    public JwtValidationResponse(boolean valid, String subject, String role, Long expiresAt) {
        this.valid = valid;
        this.subject = subject;
        this.role = role;
        this.expiresAt = expiresAt;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public Long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
