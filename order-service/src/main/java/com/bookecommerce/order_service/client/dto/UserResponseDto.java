package com.bookecommerce.order_service.client.dto;

import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String role,
        Boolean isActive
) {}
