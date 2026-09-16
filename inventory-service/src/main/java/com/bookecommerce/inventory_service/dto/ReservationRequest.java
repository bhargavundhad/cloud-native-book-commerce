package com.bookecommerce.inventory_service.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationRequest(
        @NotNull(message = "productId is required") UUID productId,
                @NotNull(message = "quantity is required") @Positive(message = "quantity must be greater than 0") Integer quantity,
                UUID orderId) {

        public ReservationRequest(UUID productId, Integer quantity) {
                this(productId, quantity, null);
        }
}
