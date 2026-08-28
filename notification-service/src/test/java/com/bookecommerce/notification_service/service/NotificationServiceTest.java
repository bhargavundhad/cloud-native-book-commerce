package com.bookecommerce.notification_service.service;

import com.bookecommerce.notification_service.dto.request.CreateNotificationRequest;
import com.bookecommerce.notification_service.dto.response.NotificationResponse;
import com.bookecommerce.notification_service.entity.Notification;
import com.bookecommerce.notification_service.entity.NotificationStatus;
import com.bookecommerce.notification_service.entity.NotificationType;
import com.bookecommerce.notification_service.exception.NotificationNotFoundException;
import com.bookecommerce.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create notification successfully")
    void testCreateNotificationSuccess() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                userId,
                NotificationType.EMAIL,
                "ORDER_CREATED",
                "Your order has been created."
        );

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(notificationId);
            return saved;
        });

        NotificationResponse response = notificationService.createNotification(request);

        assertNotNull(response);
        assertEquals(notificationId, response.id());
        assertEquals(userId, response.userId());
        assertEquals(NotificationType.EMAIL, response.type());
        assertEquals("ORDER_CREATED", response.eventType());
        assertEquals("Your order has been created.", response.message());
        assertEquals(NotificationStatus.SENT, response.status());

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should get notification by ID successfully")
    void testGetNotificationByIdSuccess() {
        Notification notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .type(NotificationType.EMAIL)
                .eventType("ORDER_CREATED")
                .message("Your order has been created.")
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.getNotificationById(notificationId);

        assertNotNull(response);
        assertEquals(notificationId, response.id());
        assertEquals(userId, response.userId());
        verify(notificationRepository, times(1)).findById(notificationId);
    }

    @Test
    @DisplayName("Should throw NotificationNotFoundException when ID does not exist")
    void testGetNotificationByIdNotFound() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.getNotificationById(notificationId));
        verify(notificationRepository, times(1)).findById(notificationId);
    }

    @Test
    @DisplayName("Should get notifications by user ID successfully")
    void testGetNotificationsByUserIdSuccess() {
        Notification notification1 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.EMAIL)
                .eventType("ORDER_CREATED")
                .message("Message 1")
                .status(NotificationStatus.SENT)
                .build();

        Notification notification2 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.SMS)
                .eventType("ORDER_CONFIRMED")
                .message("Message 2")
                .status(NotificationStatus.SENT)
                .build();

        when(notificationRepository.findByUserId(userId)).thenReturn(List.of(notification1, notification2));

        List<NotificationResponse> responses = notificationService.getNotificationsByUserId(userId);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(notificationRepository, times(1)).findByUserId(userId);
    }
}
