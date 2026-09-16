package com.bookecommerce.cart_service.integration.inventory;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryServiceResponse(
        UUID id,
        UUID productId,
        Integer quantity,
        Integer reservedQuantity,
        String status,
        LocalDateTime updatedAt) {
}
