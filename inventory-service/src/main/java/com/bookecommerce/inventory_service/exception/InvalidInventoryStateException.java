package com.bookecommerce.inventory_service.exception;

public class InvalidInventoryStateException extends RuntimeException {
    private final String errorCode;

    public InvalidInventoryStateException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
