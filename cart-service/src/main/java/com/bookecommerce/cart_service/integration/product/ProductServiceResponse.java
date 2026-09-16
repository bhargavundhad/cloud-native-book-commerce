package com.bookecommerce.cart_service.integration.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductServiceResponse(
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
