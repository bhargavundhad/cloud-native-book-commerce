package com.bookecommerce.cart_service.integration.user;

public class UserServiceIntegrationException extends RuntimeException {
    private final String errorCode;

    public UserServiceIntegrationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
