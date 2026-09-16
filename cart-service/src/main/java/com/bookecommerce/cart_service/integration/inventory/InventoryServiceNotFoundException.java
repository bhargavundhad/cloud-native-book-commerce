package com.bookecommerce.cart_service.integration.inventory;

public class InventoryServiceNotFoundException extends RuntimeException {
    private final String errorCode;

    public InventoryServiceNotFoundException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
