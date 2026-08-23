package com.bookecommerce.product_service.dto;

import com.bookecommerce.product_service.entity.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoanResponse(
        UUID id,
        UUID listingId,
        UUID borrowerId,
        BigDecimal borrowFee,
        LocalDateTime startDate,
        LocalDateTime dueDate,
        LocalDateTime returnedAt,
        LoanStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}