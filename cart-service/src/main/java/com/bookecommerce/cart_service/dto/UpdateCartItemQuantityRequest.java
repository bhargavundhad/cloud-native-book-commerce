package com.bookecommerce.cart_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateCartItemQuantityRequest(@NotNull @Positive Integer quantity) {
}
