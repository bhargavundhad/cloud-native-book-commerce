package com.bookecommerce.cart_service.integration.product;

public class ProductServiceIntegrationException extends RuntimeException {
    private final String errorCode;

    public ProductServiceIntegrationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
