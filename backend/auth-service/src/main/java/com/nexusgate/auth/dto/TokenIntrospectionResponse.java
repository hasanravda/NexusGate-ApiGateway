package com.nexusgate.auth.dto;

public class TokenIntrospectionResponse {
    
    private boolean active;
    private String subject;
    private String role;
    private Long issuedAt;
    private Long expiresAt;
    
    public TokenIntrospectionResponse() {
    }
    
    public TokenIntrospectionResponse(boolean active, String subject, String role, Long issuedAt, Long expiresAt) {
        this.active = active;
        this.subject = subject;
        this.role = role;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
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
    
    public Long getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public Long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
