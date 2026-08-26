package com.bookecommerce.cart_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.bookecommerce.cart_service.entity.CartStatus;

public record CartResponse(
        UUID id,
        UUID userId,
        CartStatus status,
        List<CartItemResponse> items,
        BigDecimal total) {
}
