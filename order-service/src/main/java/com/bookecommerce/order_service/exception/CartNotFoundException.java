package com.bookecommerce.order_service.exception;

import java.util.UUID;

public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(String message) {
        super(message);
    }

    public CartNotFoundException(UUID userId) {
        super("Active cart not found for user: " + userId);
    }
}
