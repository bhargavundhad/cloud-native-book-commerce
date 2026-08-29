package com.bookecommerce.order_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
