package com.bookecommerce.user_service.controller;

import com.bookecommerce.user_service.dto.common.ApiResponse;
import com.bookecommerce.user_service.dto.user.UpdateUserRequest;
import com.bookecommerce.user_service.dto.user.UpdateUserStatusRequest;
import com.bookecommerce.user_service.dto.user.UserResponse;
import com.bookecommerce.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.bookecommerce.user_service.dto.user.ChangePasswordRequest;
import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // =========================
    // GET MY PROFILE
    // =========================

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            Authentication authentication
    ) {

        UUID userId = (UUID) authentication.getPrincipal();

        UserResponse response =
                userService.getMyProfile(userId);

        ApiResponse<UserResponse> apiResponse =
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User fetched successfully")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    // =========================
    // UPDATE MY PROFILE
    // =========================

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request
    ) {

        UUID userId = (UUID) authentication.getPrincipal();

        UserResponse response =
                userService.updateMyProfile(
                        userId,
                        request
                );

        ApiResponse<UserResponse> apiResponse =
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User updated successfully")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    // =========================
    // ADMIN - GET USER
    // =========================

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable UUID id
    ) {

        UserResponse response =
                userService.getUserById(id);

        ApiResponse<UserResponse> apiResponse =
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User fetched successfully")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    // =========================
    // ADMIN - UPDATE USER STATUS
    // =========================

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {

        UserResponse response =
                userService.updateUserStatus(
                        id,
                        request
                );

        ApiResponse<UserResponse> apiResponse =
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User status updated successfully")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {

        List<UserResponse> users =
                userService.getAllUsers();

        ApiResponse<List<UserResponse>> response =
                ApiResponse.<List<UserResponse>>builder()
                        .success(true)
                        .message("Users fetched successfully")
                        .data(users)
                        .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
// CHANGE MY PASSWORD
// ==========================================

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        UUID userId =
                (UUID) authentication.getPrincipal();

        userService.changeMyPassword(
                userId,
                request
        );

        ApiResponse<Void> response =
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password changed successfully")
                        .data(null)
                        .build();

        return ResponseEntity.ok(response);
    }
}
