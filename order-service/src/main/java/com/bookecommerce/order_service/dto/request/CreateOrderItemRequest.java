package com.bookecommerce.order_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderItemRequest(
        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,

        @DecimalMin(value = "0.00", message = "unitPrice must be non-negative")
        BigDecimal unitPrice
) {
    public CreateOrderItemRequest(UUID productId, Integer quantity) {
        this(productId, quantity, BigDecimal.ZERO);
    }
}
