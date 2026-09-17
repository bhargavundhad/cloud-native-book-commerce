package com.bookecommerce.order_service.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponseDto(
        UUID id,
        String referenceType,
        UUID referenceId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String gatewayTransactionId,
        String status,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
