package com.bookecommerce.notification_service.dto.response;

import com.bookecommerce.notification_service.entity.NotificationStatus;
import com.bookecommerce.notification_service.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationType type,
        String eventType,
        String message,
        NotificationStatus status,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {}
