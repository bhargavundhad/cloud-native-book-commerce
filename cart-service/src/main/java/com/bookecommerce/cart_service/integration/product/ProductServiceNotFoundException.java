package com.bookecommerce.cart_service.integration.product;

public class ProductServiceNotFoundException extends RuntimeException {
    private final String errorCode;

    public ProductServiceNotFoundException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
