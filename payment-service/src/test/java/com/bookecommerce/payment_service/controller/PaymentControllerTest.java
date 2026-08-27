package com.bookecommerce.payment_service.controller;

import com.bookecommerce.payment_service.dto.request.CreatePaymentRequest;
import com.bookecommerce.payment_service.dto.response.PaymentResponse;
import com.bookecommerce.payment_service.entity.PaymentStatus;
import com.bookecommerce.payment_service.exception.GlobalExceptionHandler;
import com.bookecommerce.payment_service.exception.PaymentNotFoundException;
import com.bookecommerce.payment_service.service.PaymentService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private UUID paymentId;
    private UUID orderId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        paymentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/payments should create payment and return HTTP 201 CREATED")
    void testCreateMockPaymentSuccess() throws Exception {
        String jsonPayload = """
                {
                    "referenceType": "ORDER",
                    "referenceId": "%s",
                    "userId": "%s",
                    "amount": 1198.00,
                    "currency": "INR",
                    "paymentMethod": "MOCK"
                }
                """.formatted(orderId, userId);

        PaymentResponse mockResponse = new PaymentResponse(
                paymentId,
                "ORDER",
                orderId,
                userId,
                new BigDecimal("1198.00"),
                "INR",
                "MOCK",
                "MOCK-TXN-12345",
                PaymentStatus.SUCCESS,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.createMockPayment(any(CreatePaymentRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(1198.00));
    }

    @Test
    @DisplayName("POST /api/payments should return HTTP 400 BAD REQUEST when amount is invalid")
    void testCreateMockPaymentInvalidAmount() throws Exception {
        String jsonPayload = """
                {
                    "referenceType": "ORDER",
                    "referenceId": "%s",
                    "userId": "%s",
                    "amount": 0.00,
                    "currency": "INR",
                    "paymentMethod": "MOCK"
                }
                """.formatted(orderId, userId);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/payments/{id} should return payment and HTTP 200 OK")
    void testGetPaymentByIdSuccess() throws Exception {
        PaymentResponse mockResponse = new PaymentResponse(
                paymentId,
                "ORDER",
                orderId,
                userId,
                new BigDecimal("1198.00"),
                "INR",
                "MOCK",
                "MOCK-TXN-12345",
                PaymentStatus.SUCCESS,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.getPaymentById(paymentId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()));
    }

    @Test
    @DisplayName("GET /api/payments/{id} should return HTTP 404 NOT FOUND when payment does not exist")
    void testGetPaymentByIdNotFound() throws Exception {
        when(paymentService.getPaymentById(paymentId)).thenThrow(new PaymentNotFoundException(paymentId));

        mockMvc.perform(get("/api/payments/" + paymentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PAYMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/payments/order/{orderId} should return payment by order ID")
    void testGetPaymentByOrderReferenceSuccess() throws Exception {
        PaymentResponse mockResponse = new PaymentResponse(
                paymentId,
                "ORDER",
                orderId,
                userId,
                new BigDecimal("1198.00"),
                "INR",
                "MOCK",
                "MOCK-TXN-12345",
                PaymentStatus.SUCCESS,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.getPaymentByOrderReference(orderId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/payments/order/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").value(orderId.toString()));
    }

    @Test
    @DisplayName("POST /api/payments/{id}/refund should refund payment and return HTTP 200 OK")
    void testRefundPaymentSuccess() throws Exception {
        PaymentResponse mockResponse = new PaymentResponse(
                paymentId,
                "ORDER",
                orderId,
                userId,
                new BigDecimal("1198.00"),
                "INR",
                "MOCK",
                "MOCK-TXN-12345",
                PaymentStatus.REFUNDED,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.refundPayment(paymentId)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments/" + paymentId + "/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }
}
