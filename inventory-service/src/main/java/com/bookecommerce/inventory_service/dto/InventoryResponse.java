package com.bookecommerce.inventory_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.bookecommerce.inventory_service.entity.InventoryStatus;

public record InventoryResponse(
        UUID id,
        UUID productId,
        Integer quantity,
        Integer reservedQuantity,
        InventoryStatus status,
        LocalDateTime updatedAt) {
}
