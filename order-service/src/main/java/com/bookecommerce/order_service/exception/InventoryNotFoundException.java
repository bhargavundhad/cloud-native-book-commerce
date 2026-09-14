package com.bookecommerce.order_service.exception;

import java.util.UUID;

public class InventoryNotFoundException extends RuntimeException {

    private final UUID productId;

    public InventoryNotFoundException(UUID productId) {
        super("Inventory not found for productId: " + productId);
        this.productId = productId;
    }

    public InventoryNotFoundException(String message, UUID productId) {
        super(message);
        this.productId = productId;
    }

    public UUID getProductId() {
        return productId;
    }
}
