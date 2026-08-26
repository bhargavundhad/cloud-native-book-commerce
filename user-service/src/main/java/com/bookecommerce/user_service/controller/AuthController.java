package com.bookecommerce.user_service.controller;

import com.bookecommerce.user_service.dto.auth.LoginRequest;
import com.bookecommerce.user_service.dto.auth.LoginResponse;
import com.bookecommerce.user_service.dto.auth.RegisterRequest;
import com.bookecommerce.user_service.dto.auth.RegisterResponse;
import com.bookecommerce.user_service.service.AuthenticationService;
import com.bookecommerce.user_service.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response =
                registrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response =
                authenticationService.login(request);

        return ResponseEntity.ok(response);
    }
}