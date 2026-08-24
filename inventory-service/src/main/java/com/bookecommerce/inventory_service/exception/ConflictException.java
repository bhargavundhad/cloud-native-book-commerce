package com.bookecommerce.inventory_service.exception;

public class ConflictException extends RuntimeException {
    private final String errorCode;

    public ConflictException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
