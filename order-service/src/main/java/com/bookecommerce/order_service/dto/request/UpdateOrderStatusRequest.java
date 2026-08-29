package com.bookecommerce.order_service.dto.request;

import com.bookecommerce.order_service.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "status is required")
        OrderStatus status
) {}
