package com.bookecommerce.product_service.dto;

import com.bookecommerce.product_service.entity.BookCondition;
import com.bookecommerce.product_service.entity.ListingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookListingResponse(
        UUID id,
        UUID bookId,
        UUID ownerId,
        BookCondition condition,
        BigDecimal borrowFee,
        Integer borrowDurationDays,
        ListingStatus status,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}