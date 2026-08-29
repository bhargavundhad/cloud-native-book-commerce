package com.bookecommerce.payment_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotBlank(message = "referenceType must not be null or blank")
        String referenceType,

        @NotNull(message = "referenceId must not be null")
        UUID referenceId,

        UUID userId,

        @NotNull(message = "amount must not be null")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "currency must not be null or blank")
        String currency,

        @NotBlank(message = "paymentMethod must not be null or blank")
        String paymentMethod
) {}
