package com.bookecommerce.user_service.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private String tokenType;

    private long expiresIn;

    private UUID userId;

    private String email;

    private String firstName;

    private String lastName;

    private String role;
}