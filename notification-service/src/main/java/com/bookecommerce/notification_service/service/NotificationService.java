package com.bookecommerce.notification_service.service;

import com.bookecommerce.notification_service.dto.request.CreateNotificationRequest;
import com.bookecommerce.notification_service.dto.response.NotificationResponse;
import com.bookecommerce.notification_service.entity.Notification;
import com.bookecommerce.notification_service.entity.NotificationStatus;
import com.bookecommerce.notification_service.exception.NotificationNotFoundException;
import com.bookecommerce.notification_service.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationResponse createNotification(CreateNotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateNotificationRequest cannot be null");
        }
        if (request.userId() == null) {
            throw new IllegalArgumentException("userId is required");
        }

        LocalDateTime now = LocalDateTime.now();

        Notification notification = Notification.builder()
                .userId(request.userId())
                .type(request.type())
                .eventType(request.eventType())
                .message(request.message())
                .status(NotificationStatus.SENT)
                .sentAt(now)
                .build();

        Notification saved = notificationRepository.save(notification);
        return mapToNotificationResponse(saved);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Notification id cannot be null");
        }
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        return mapToNotificationResponse(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        List<Notification> notifications = notificationRepository.findByUserId(userId);
        return notifications.stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());
    }

    private NotificationResponse mapToNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getEventType(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getSentAt(),
                notification.getCreatedAt()
        );
    }
}
