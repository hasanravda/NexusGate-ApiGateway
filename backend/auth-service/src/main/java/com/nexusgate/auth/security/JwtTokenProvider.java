package com.nexusgate.auth.security;

import com.nexusgate.auth.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {
    
    private final JwtConfig jwtConfig;
    
    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Generate JWT token with custom claims
     */
    public String generateToken(String subject, String role, Map<String, Object> additionalClaims) {
        Map<String, Object> claims = new HashMap<>();
        if (additionalClaims != null) {
            claims.putAll(additionalClaims);
        }
        claims.put("role", role);
        
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiryDate = new Date(nowMillis + jwtConfig.getAccessTokenExpiration());
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtConfig.getIssuer())
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }
    
    /**
     * Generate access token
     */
    public String generateAccessToken(String subject, String role) {
        return generateToken(subject, role, null);
    }
    
    /**
     * Generate refresh token with extended expiry
     */
    public String generateRefreshToken(String subject, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("type", "REFRESH");
        
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiryDate = new Date(nowMillis + jwtConfig.getRefreshTokenExpiration());
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtConfig.getIssuer())
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }
    
    /**
     * Parse and validate JWT token
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SignatureException e) {
            throw new JwtException("Invalid JWT signature");
        } catch (MalformedJwtException e) {
            throw new JwtException("Invalid JWT token");
        } catch (ExpiredJwtException e) {
            throw new JwtException("JWT token is expired");
        } catch (UnsupportedJwtException e) {
            throw new JwtException("JWT token is unsupported");
        } catch (IllegalArgumentException e) {
            throw new JwtException("JWT claims string is empty");
        }
    }
    
    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
    
    /**
     * Extract subject from token
     */
    public String getSubjectFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
    
    /**
     * Extract role from token
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }
    
    /**
     * Extract expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }
    
    /**
     * Extract issued at date from token
     */
    public Date getIssuedAtFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getIssuedAt();
    }
    
    /**
     * Extract issuer from token
     */
    public String getIssuerFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getIssuer();
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
    
    /**
     * Alias methods for JwtService compatibility
     */
    public boolean isTokenValid(String token) {
        return validateToken(token);
    }
    
    public String getSubject(String token) {
        return getSubjectFromToken(token);
    }
    
    public String getRole(String token) {
        return getRoleFromToken(token);
    }
    
    public Date getExpirationDate(String token) {
        return getExpirationDateFromToken(token);
    }
    
    public Claims validateAndParseClaims(String token) {
        return parseToken(token);
    }
}
