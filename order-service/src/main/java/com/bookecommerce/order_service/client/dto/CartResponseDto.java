package com.bookecommerce.order_service.client.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponseDto(
        UUID id,
        UUID userId,
        String status,
        List<CartItemResponseDto> items,
        BigDecimal total
) {}
