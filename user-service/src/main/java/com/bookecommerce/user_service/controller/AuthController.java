package com.bookecommerce.user_service.controller;

import com.bookecommerce.user_service.dto.auth.LoginRequest;
import com.bookecommerce.user_service.dto.auth.LoginResponse;
import com.bookecommerce.user_service.dto.auth.RegisterRequest;
import com.bookecommerce.user_service.dto.auth.RegisterResponse;
import com.bookecommerce.user_service.dto.auth.TokenValidationResponse;
import com.bookecommerce.user_service.dto.common.ApiResponse;
import com.bookecommerce.user_service.service.AuthenticationService;
import com.bookecommerce.user_service.service.RegistrationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;


    // =========================
    // REGISTER
    // =========================

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        RegisterResponse response =
                registrationService.register(request);

        ApiResponse<RegisterResponse> apiResponse =
                ApiResponse.<RegisterResponse>builder()
                        .success(true)
                        .message("User registered successfully")
                        .data(response)
                        .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(apiResponse);
    }


    // =========================
    // LOGIN
    // =========================

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response =
                authenticationService.login(request);

        ApiResponse<LoginResponse> apiResponse =
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .message("Login successful")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }


    // =========================
    // VALIDATE JWT
    // =========================

    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(
            Authentication authentication
    ) {

        String userId = authentication.getName();

        String role = authentication
                .getAuthorities()
                .stream()
                .findFirst()
                .map(authority -> authority.getAuthority())
                .orElse(null);

        TokenValidationResponse response =
                TokenValidationResponse.builder()
                        .valid(true)
                        .userId(userId)
                        .role(role)
                        .build();

        return ResponseEntity.ok(response);
    }
}