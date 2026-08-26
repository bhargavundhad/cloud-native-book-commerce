package com.bookecommerce.cart_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantity,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal unitPrice) {
}
