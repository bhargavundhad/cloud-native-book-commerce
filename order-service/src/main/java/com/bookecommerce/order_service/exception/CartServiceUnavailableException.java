package com.bookecommerce.order_service.exception;

public class CartServiceUnavailableException extends RuntimeException {
    public CartServiceUnavailableException(String message) {
        super(message);
    }

    public CartServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
