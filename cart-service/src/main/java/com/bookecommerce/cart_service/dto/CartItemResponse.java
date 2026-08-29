package com.bookecommerce.cart_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(UUID id, UUID productId, Integer quantity, BigDecimal unitPrice) {
}
