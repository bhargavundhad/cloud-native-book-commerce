package com.bookecommerce.inventory_service.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryCreateRequest(
        @NotNull(message = "productId is required") UUID productId,
        @NotNull(message = "quantity is required") @PositiveOrZero(message = "quantity must be greater than or equal to 0") Integer quantity) {
}
