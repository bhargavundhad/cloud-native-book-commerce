package com.bookecommerce.product_service.dto;

import com.bookecommerce.product_service.entity.BookCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BookListingUpdateRequest(
        @NotNull BookCondition condition,
        @NotNull @DecimalMin("0.00") BigDecimal borrowFee,
        @NotNull @Positive Integer borrowDurationDays,
        String description) {
}