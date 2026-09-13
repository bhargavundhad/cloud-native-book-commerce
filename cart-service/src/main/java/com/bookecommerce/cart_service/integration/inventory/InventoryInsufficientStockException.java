package com.bookecommerce.cart_service.integration.inventory;

public class InventoryInsufficientStockException extends RuntimeException {
    private final String errorCode;

    public InventoryInsufficientStockException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
