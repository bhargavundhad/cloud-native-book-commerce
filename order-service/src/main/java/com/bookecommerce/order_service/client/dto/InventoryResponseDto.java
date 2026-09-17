package com.bookecommerce.order_service.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponseDto(
        UUID id,
        UUID productId,
        Integer quantity,
        Integer reservedQuantity,
        String status,
        LocalDateTime updatedAt,
        UUID reservationId,
        UUID orderId,
        Integer reservationQuantity,
        String reservationStatus,
        LocalDateTime reservationCreatedAt,
        LocalDateTime reservationUpdatedAt
) {
    public InventoryResponseDto(UUID id, UUID productId, Integer quantity, Integer reservedQuantity, String status, LocalDateTime updatedAt) {
        this(id, productId, quantity, reservedQuantity, status, updatedAt,
                null, null, null, null, null, null);
    }
}
