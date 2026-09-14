package com.bookecommerce.inventory_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.bookecommerce.inventory_service.entity.InventoryStatus;
import com.bookecommerce.inventory_service.entity.ReservationStatus;

public record InventoryResponse(
        UUID id,
        UUID productId,
        Integer quantity,
        Integer reservedQuantity,
        InventoryStatus status,
        LocalDateTime updatedAt,
        UUID reservationId,
        UUID orderId,
        Integer reservationQuantity,
        ReservationStatus reservationStatus,
        LocalDateTime reservationCreatedAt,
        LocalDateTime reservationUpdatedAt) {

    public InventoryResponse(UUID id, UUID productId, Integer quantity, Integer reservedQuantity,
            InventoryStatus status, LocalDateTime updatedAt) {
        this(id, productId, quantity, reservedQuantity, status, updatedAt,
                null, null, null, null, null, null);
    }
}
