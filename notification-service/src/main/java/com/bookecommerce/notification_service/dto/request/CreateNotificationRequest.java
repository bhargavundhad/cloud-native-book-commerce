package com.bookecommerce.notification_service.dto.request;

import com.bookecommerce.notification_service.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateNotificationRequest(
        @NotNull(message = "userId is required")
        UUID userId,

        @NotNull(message = "type is required")
        NotificationType type,

        @NotBlank(message = "eventType is required")
        String eventType,

        @NotBlank(message = "message is required")
        String message
) {}
