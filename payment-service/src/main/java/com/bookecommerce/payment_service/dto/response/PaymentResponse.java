package com.bookecommerce.payment_service.dto.response;

import com.bookecommerce.payment_service.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String referenceType,
        UUID referenceId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String gatewayTransactionId,
        PaymentStatus status,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
