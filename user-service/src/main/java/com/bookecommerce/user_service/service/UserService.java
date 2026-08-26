package com.bookecommerce.user_service.service;

import com.bookecommerce.user_service.dto.user.UserResponse;
import com.bookecommerce.user_service.entity.User;
import com.bookecommerce.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bookecommerce.user_service.dto.user.UpdateUserRequest;
import com.bookecommerce.user_service.dto.user.UpdateUserStatusRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        return mapToResponse(user);
    }

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

    @Transactional
    public UserResponse updateMyProfile(
            UUID userId,
            UpdateUserRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        String newEmail = request.getEmail()
                .trim()
                .toLowerCase();

        if (!user.getEmail().equalsIgnoreCase(newEmail)
                && userRepository.existsByEmail(newEmail)) {

            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().isBlank()) {

            String newPhone =
                    request.getPhoneNumber().trim();

            if (!newPhone.equals(user.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(newPhone)) {

                throw new IllegalArgumentException(
                        "Phone number is already registered"
                );
            }

            user.setPhoneNumber(newPhone);

        } else {
            user.setPhoneNumber(null);
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(newEmail);

        User updatedUser = userRepository.save(user);

        return mapToResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(
            UUID userId,
            UpdateUserStatusRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        user.setIsActive(request.getIsActive());

        User updatedUser = userRepository.save(user);

        return mapToResponse(updatedUser);
    }
}