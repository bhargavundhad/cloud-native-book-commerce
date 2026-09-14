package com.bookecommerce.order_service.exception;

public class CartNotActiveException extends RuntimeException {
    public CartNotActiveException(String message) {
        super(message);
    }
}
