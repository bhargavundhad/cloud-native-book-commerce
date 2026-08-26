package com.bookecommerce.user_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expirationMs = expirationMs;
    }

    /**
     * Generate JWT token for a user.
     */
    public String generateToken(
            UUID userId,
            String email,
            String role
    ) {

        Date issuedAt = new Date();
        Date expiration = new Date(
                issuedAt.getTime() + expirationMs
        );

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extract user ID from JWT.
     */
    public UUID extractUserId(String token) {

        String subject = extractClaims(token).getSubject();

        return UUID.fromString(subject);
    }

    /**
     * Extract email from JWT.
     */
    public String extractEmail(String token) {

        return extractClaims(token)
                .get("email", String.class);
    }

    /**
     * Extract role from JWT.
     */
    public String extractRole(String token) {

        return extractClaims(token)
                .get("role", String.class);
    }

    /**
     * Check whether JWT is valid.
     */
    public boolean isTokenValid(String token) {

        try {
            extractClaims(token);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Parse JWT claims.
     */
    private Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}