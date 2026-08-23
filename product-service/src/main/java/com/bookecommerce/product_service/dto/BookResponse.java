package com.bookecommerce.product_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String isbn,
        String title,
        String description,
        UUID authorId,
        UUID categoryId,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}