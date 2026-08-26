package com.bookecommerce.cart_service.exception;

import java.util.Map;

public record ApiError(boolean success, String message, String errorCode, Map<String, String> errors) {

    public ApiError(boolean success, String message, String errorCode) {
        this(success, message, errorCode, Map.of());
    }
}
