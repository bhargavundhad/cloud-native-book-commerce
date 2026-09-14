package com.bookecommerce.order_service.security;

import com.bookecommerce.order_service.exception.UserUnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtTokenValidator {

    private final SecretKey secretKey;

    public JwtTokenValidator(
            @Value("${app.jwt.secret:book-ecommerce-user-service-jwt-secret-key-2026}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public UUID extractUserIdFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UserUnauthorizedException("Missing or invalid Authorization header");
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw new UserUnauthorizedException("Empty JWT token");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new UserUnauthorizedException("JWT does not contain user ID subject");
            }
            return UUID.fromString(subject);
        } catch (UserUnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UserUnauthorizedException("Invalid or expired JWT token");
        }
    }

    public String extractRoleFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring(7).trim();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("role", String.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
