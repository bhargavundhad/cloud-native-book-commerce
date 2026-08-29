package com.bookecommerce.user_service.service;

import com.bookecommerce.user_service.dto.auth.LoginRequest;
import com.bookecommerce.user_service.dto.auth.LoginResponse;
import com.bookecommerce.user_service.entity.User;
import com.bookecommerce.user_service.repository.UserRepository;
import com.bookecommerce.user_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadCredentialsException(
                    "User account is inactive"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        String role = user.getRole().getName();

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                role
        );

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(role)
                .build();
    }
}