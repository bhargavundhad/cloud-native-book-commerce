package com.bookecommerce.user_service.security;

import com.bookecommerce.user_service.dto.common.ApiErrorResponse;
import com.bookecommerce.user_service.entity.User;
import com.bookecommerce.user_service.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        /*
         * ==========================================
         * NO AUTHORIZATION HEADER
         * ==========================================
         *
         * Public endpoints can continue normally.
         *
         * For protected endpoints, Spring Security will
         * later return 401 through the AuthenticationEntryPoint.
         */
        if (authorizationHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * ==========================================
         * INVALID AUTHORIZATION HEADER
         * ==========================================
         */

        if (!authorizationHeader.startsWith("Bearer ")) {

            sendUnauthorizedResponse(
                    response,
                    "Invalid Authorization header",
                    "INVALID_JWT"
            );

            return;
        }

        String token =
                authorizationHeader.substring(7).trim();

        /*
         * ==========================================
         * EMPTY TOKEN
         * ==========================================
         */

        if (token.isEmpty()) {

            sendUnauthorizedResponse(
                    response,
                    "Invalid JWT",
                    "INVALID_JWT"
            );

            return;
        }

        try {

            /*
             * ==========================================
             * JWT VALIDATION
             * ==========================================
             *
             * extractUserId() internally parses the JWT.
             *
             * This verifies:
             *
             * 1. JWT structure
             * 2. JWT signature
             * 3. JWT expiration
             */

            UUID userId =
                    jwtService.extractUserId(token);

            /*
             * ==========================================
             * USER EXISTENCE CHECK
             * ==========================================
             *
             * The JWT may still be valid even if the user
             * has been deleted from the database.
             */

            User user =
                    userRepository.findById(userId)
                            .orElse(null);

            if (user == null) {

                SecurityContextHolder.clearContext();

                sendUnauthorizedResponse(
                        response,
                        "User not found",
                        "USER_NOT_FOUND"
                );

                return;
            }

            /*
             * ==========================================
             * CURRENT ACTIVE STATUS CHECK
             * ==========================================
             *
             * This is important because a previously issued
             * JWT must NOT allow an inactive user to access
             * protected endpoints.
             */

            if (!Boolean.TRUE.equals(user.getIsActive())) {

                SecurityContextHolder.clearContext();

                sendUnauthorizedResponse(
                        response,
                        "User account is inactive",
                        "USER_INACTIVE"
                );

                return;
            }

            /*
             * ==========================================
             * USE CURRENT DATABASE USER INFORMATION
             * ==========================================
             *
             * We use the current database role instead of
             * trusting the role stored inside an old JWT.
             */

            String email =
                    user.getEmail();

            String role =
                    user.getRole().getName();

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(
                            "ROLE_" + role
                    );

            /*
             * ==========================================
             * CREATE AUTHENTICATION
             * ==========================================
             */

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(authority)
                    );

            authentication.setDetails(email);

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            /*
             * Continue request.
             */

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException exception) {

            /*
             * Invalid JWT, expired JWT, malformed JWT,
             * invalid signature, invalid UUID, etc.
             */

            SecurityContextHolder.clearContext();

            sendUnauthorizedResponse(
                    response,
                    "Invalid or expired JWT",
                    "INVALID_JWT"
            );
        }
    }

    /*
     * ==========================================
     * SEND STRUCTURED 401 RESPONSE
     * ==========================================
     */

    private void sendUnauthorizedResponse(
            HttpServletResponse response,
            String message,
            String errorCode
    ) throws IOException {

        ApiErrorResponse errorResponse =
                ApiErrorResponse.builder()
                        .success(false)
                        .message(message)
                        .errorCode(errorCode)
                        .build();

        response.setStatus(
                HttpStatus.UNAUTHORIZED.value()
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.getWriter().write(
                objectMapper.writeValueAsString(errorResponse)
        );
    }
}