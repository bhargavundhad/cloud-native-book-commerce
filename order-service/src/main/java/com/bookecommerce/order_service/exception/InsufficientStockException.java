package com.bookecommerce.order_service.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    private final UUID productId;

    public InsufficientStockException(UUID productId) {
        super("Insufficient stock for productId: " + productId);
        this.productId = productId;
    }

    public InsufficientStockException(String message) {
        super(message);
        this.productId = null;
    }

    public InsufficientStockException(String message, UUID productId) {
        super(message);
        this.productId = productId;
    }

    public UUID getProductId() {
        return productId;
    }
}
