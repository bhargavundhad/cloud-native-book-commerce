package com.bookecommerce.order_service.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponseDto(
        UUID id,
        String isbn,
        String title,
        String description,
        UUID authorId,
        UUID categoryId,
        BigDecimal price
) {}
