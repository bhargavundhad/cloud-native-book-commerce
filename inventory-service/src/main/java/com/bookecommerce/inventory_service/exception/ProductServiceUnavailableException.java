package com.bookecommerce.inventory_service.exception;

public class ProductServiceUnavailableException extends RuntimeException {
    private final String errorCode;

    public ProductServiceUnavailableException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
