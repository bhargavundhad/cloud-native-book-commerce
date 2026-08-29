package com.bookecommerce.user_service.service;

import com.bookecommerce.user_service.dto.user.UpdateUserRequest;
import com.bookecommerce.user_service.dto.user.UpdateUserStatusRequest;
import com.bookecommerce.user_service.dto.user.UserResponse;
import com.bookecommerce.user_service.entity.User;
import com.bookecommerce.user_service.exception.DuplicateEmailException;
import com.bookecommerce.user_service.exception.DuplicatePhoneException;
import com.bookecommerce.user_service.exception.UserNotFoundException;
import com.bookecommerce.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.bookecommerce.user_service.dto.user.ChangePasswordRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // ==========================================
    // GET MY PROFILE
    // ==========================================

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found"
                        )
                );

        return mapToResponse(user);
    }

    // ==========================================
    // UPDATE MY PROFILE
    // ==========================================

    @Transactional
    public UserResponse updateMyProfile(
            UUID userId,
            UpdateUserRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found"
                        )
                );

        String newEmail = request.getEmail()
                .trim()
                .toLowerCase();

        // Check duplicate email
        if (!user.getEmail().equalsIgnoreCase(newEmail)
                && userRepository.existsByEmail(newEmail)) {

            throw new DuplicateEmailException(
                    "Email is already registered"
            );
        }

        // Check duplicate phone
        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().isBlank()) {

            String newPhone =
                    request.getPhoneNumber().trim();

            if (!newPhone.equals(user.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(newPhone)) {

                throw new DuplicatePhoneException(
                        "Phone number is already registered"
                );
            }

            user.setPhoneNumber(newPhone);

        } else {
            user.setPhoneNumber(null);
        }

        user.setFirstName(
                request.getFirstName().trim()
        );

        user.setLastName(
                request.getLastName().trim()
        );

        user.setEmail(newEmail);

        User updatedUser =
                userRepository.save(user);

        return mapToResponse(updatedUser);
    }

    // ==========================================
    // GET USER BY ID - ADMIN
    // ==========================================

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found"
                        )
                );

        return mapToResponse(user);
    }

    // ==========================================
    // UPDATE USER STATUS - ADMIN
    // ==========================================

    @Transactional
    public UserResponse updateUserStatus(
            UUID userId,
            UpdateUserStatusRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found"
                        )
                );

        user.setIsActive(
                request.getIsActive()
        );

        User updatedUser =
                userRepository.save(user);

        return mapToResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ==========================================
// CHANGE MY PASSWORD
// ==========================================

    @Transactional
    public void changeMyPassword(
            UUID userId,
            ChangePasswordRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found"
                        )
                );

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadCredentialsException(
                    "User account is inactive"
            );
        }

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPasswordHash()
        )) {

            throw new BadCredentialsException(
                    "Current password is incorrect"
            );
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPasswordHash()
        )) {

            throw new IllegalArgumentException(
                    "New password must be different from current password"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);
    }

    // ==========================================
    // MAP USER TO RESPONSE
    // ==========================================

    private UserResponse mapToResponse(User user) {

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName())
                .isActive(user.getIsActive())
                .build();
    }

}