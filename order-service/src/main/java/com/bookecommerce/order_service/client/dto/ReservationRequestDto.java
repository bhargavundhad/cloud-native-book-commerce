package com.bookecommerce.order_service.client.dto;

import java.util.UUID;

public record ReservationRequestDto(
        UUID productId,
        Integer quantity,
        UUID orderId
) {
    public ReservationRequestDto(UUID productId, Integer quantity) {
        this(productId, quantity, null);
    }
}
