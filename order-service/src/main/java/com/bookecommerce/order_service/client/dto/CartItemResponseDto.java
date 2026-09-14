package com.bookecommerce.order_service.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponseDto(
        UUID id,
        UUID productId,
        Integer quantity,
        BigDecimal unitPrice
) {}
