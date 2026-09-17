package com.bookecommerce.order_service.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestDto(
        String referenceType,
        UUID referenceId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String paymentMethod
) {
    public PaymentRequestDto(UUID referenceId, UUID userId, BigDecimal amount, String currency, String paymentMethod) {
        this("ORDER", referenceId, userId, amount, currency, paymentMethod);
    }
}
