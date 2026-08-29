package com.bookecommerce.inventory_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StockUpdateRequest(
        @NotNull(message = "quantity is required") @PositiveOrZero(message = "quantity must be greater than or equal to 0") Integer quantity) {
}
