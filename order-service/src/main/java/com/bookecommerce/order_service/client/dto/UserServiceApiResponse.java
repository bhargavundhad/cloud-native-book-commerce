package com.bookecommerce.order_service.client.dto;

public record UserServiceApiResponse<T>(
        boolean success,
        String message,
        T data
) {}
