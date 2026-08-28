package com.bookecommerce.notification_service.controller;

import com.bookecommerce.notification_service.dto.request.CreateNotificationRequest;
import com.bookecommerce.notification_service.dto.response.NotificationResponse;
import com.bookecommerce.notification_service.entity.NotificationStatus;
import com.bookecommerce.notification_service.entity.NotificationType;
import com.bookecommerce.notification_service.exception.GlobalExceptionHandler;
import com.bookecommerce.notification_service.exception.NotificationNotFoundException;
import com.bookecommerce.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/notifications should create notification and return HTTP 201 CREATED")
    void testCreateNotificationSuccess() throws Exception {
        String jsonPayload = """
                {
                    "userId": "%s",
                    "type": "EMAIL",
                    "eventType": "ORDER_CREATED",
                    "message": "Your order has been created."
                }
                """.formatted(userId);

        NotificationResponse mockResponse = new NotificationResponse(
                notificationId,
                userId,
                NotificationType.EMAIL,
                "ORDER_CREATED",
                "Your order has been created.",
                NotificationStatus.SENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(notificationService.createNotification(any(CreateNotificationRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notificationId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.type").value("EMAIL"))
                .andExpect(jsonPath("$.eventType").value("ORDER_CREATED"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("POST /api/notifications should return HTTP 400 BAD REQUEST when message is blank")
    void testCreateNotificationInvalidMessage() throws Exception {
        String jsonPayload = """
                {
                    "userId": "%s",
                    "type": "EMAIL",
                    "eventType": "ORDER_CREATED",
                    "message": ""
                }
                """.formatted(userId);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/notifications/{id} should return notification and HTTP 200 OK")
    void testGetNotificationByIdSuccess() throws Exception {
        NotificationResponse mockResponse = new NotificationResponse(
                notificationId,
                userId,
                NotificationType.EMAIL,
                "ORDER_CREATED",
                "Your order has been created.",
                NotificationStatus.SENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(notificationService.getNotificationById(notificationId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/notifications/" + notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId.toString()));
    }

    @Test
    @DisplayName("GET /api/notifications/{id} should return HTTP 404 NOT FOUND when notification does not exist")
    void testGetNotificationByIdNotFound() throws Exception {
        when(notificationService.getNotificationById(notificationId)).thenThrow(new NotificationNotFoundException(notificationId));

        mockMvc.perform(get("/api/notifications/" + notificationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/notifications/user/{userId} should return user notifications")
    void testGetNotificationsByUserIdSuccess() throws Exception {
        NotificationResponse mockResponse = new NotificationResponse(
                notificationId,
                userId,
                NotificationType.EMAIL,
                "ORDER_CREATED",
                "Your order has been created.",
                NotificationStatus.SENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(notificationService.getNotificationsByUserId(userId)).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/notifications/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(notificationId.toString()))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }
}
