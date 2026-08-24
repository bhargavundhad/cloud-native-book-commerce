package com.bookecommerce.product_service.dto;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookUpdateRequest(
        @NotBlank @Size(max = 20) String isbn,
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull UUID authorId,
        @NotNull UUID categoryId,
        @NotNull @DecimalMin(value = "0.00") BigDecimal price) {
}