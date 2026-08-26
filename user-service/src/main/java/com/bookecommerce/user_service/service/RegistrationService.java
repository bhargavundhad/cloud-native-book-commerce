package com.bookecommerce.user_service.service;

import com.bookecommerce.user_service.dto.auth.RegisterRequest;
import com.bookecommerce.user_service.dto.auth.RegisterResponse;
import com.bookecommerce.user_service.entity.Role;
import com.bookecommerce.user_service.entity.User;
import com.bookecommerce.user_service.repository.RoleRepository;
import com.bookecommerce.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().isBlank()
                && userRepository.existsByPhoneNumber(
                request.getPhoneNumber().trim()
        )) {
            throw new IllegalArgumentException(
                    "Phone number is already registered"
            );
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "USER role does not exist in database"
                        )
                );

        User user = User.builder()
                .role(userRole)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .passwordHash(
                        passwordEncoder.encode(request.getPassword())
                )
                .phoneNumber(
                        request.getPhoneNumber() == null
                                ? null
                                : request.getPhoneNumber().trim()
                )
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .phoneNumber(savedUser.getPhoneNumber())
                .role(savedUser.getRole().getName())
                .isActive(savedUser.getIsActive())
                .build();
    }
}