package com.bookecommerce.cart_service.integration.user;

public class UserServiceNotFoundException extends RuntimeException {
    private final String errorCode;

    public UserServiceNotFoundException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
