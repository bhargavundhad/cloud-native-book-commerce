package com.bookecommerce.cart_service.integration.inventory;

public class InventoryServiceIntegrationException extends RuntimeException {
    private final String errorCode;

    public InventoryServiceIntegrationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
