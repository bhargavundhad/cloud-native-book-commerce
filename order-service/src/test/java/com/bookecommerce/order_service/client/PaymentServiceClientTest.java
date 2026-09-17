package com.bookecommerce.order_service.client;

import com.bookecommerce.order_service.client.dto.PaymentRequestDto;
import com.bookecommerce.order_service.client.dto.PaymentResponseDto;
import com.bookecommerce.order_service.exception.PaymentFailedException;
import com.bookecommerce.order_service.exception.PaymentServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PaymentServiceClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private PaymentServiceClient paymentServiceClient;
    private static final String BASE_URL = "http://localhost:8086";

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        paymentServiceClient = new PaymentServiceClient(restClientBuilder.build(), BASE_URL);
    }

    @Test
    @DisplayName("Should forward Authorization header and return PaymentResponseDto when Payment Service processes payment with 201 CREATED")
    void testCreatePaymentSuccessWithBearerToken() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        String jsonResponse = """
                {
                    "id": "%s",
                    "referenceType": "ORDER",
                    "referenceId": "%s",
                    "userId": "%s",
                    "amount": 1497.00,
                    "currency": "INR",
                    "paymentMethod": "MOCK",
                    "gatewayTransactionId": "MOCK-TXN-12345",
                    "status": "SUCCESS",
                    "paidAt": "2026-09-14T23:00:00",
                    "createdAt": "2026-09-14T23:00:00",
                    "updatedAt": "2026-09-14T23:00:00"
                }
                """.formatted(paymentId, orderId, userId);

        PaymentRequestDto requestDto = new PaymentRequestDto(orderId, userId, new BigDecimal("1497.00"), "INR", "MOCK");

        mockServer.expect(requestTo(BASE_URL + "/api/payments"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(jsonPath("$.referenceType").value("ORDER"))
                .andExpect(jsonPath("$.referenceId").value(orderId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.amount").value(1497.00))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.paymentMethod").value("MOCK"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(jsonResponse));

        PaymentResponseDto response = paymentServiceClient.createPayment(requestDto, bearerToken);

        assertNotNull(response);
        assertEquals(paymentId, response.id());
        assertEquals("ORDER", response.referenceType());
        assertEquals(orderId, response.referenceId());
        assertEquals(userId, response.userId());
        assertEquals(new BigDecimal("1497.00"), response.amount());
        assertEquals("SUCCESS", response.status());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw PaymentFailedException when Payment Service returns 400 BAD_REQUEST")
    void testCreatePaymentBadRequest() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        PaymentRequestDto requestDto = new PaymentRequestDto(orderId, userId, new BigDecimal("1497.00"), "INR", "MOCK");

        mockServer.expect(requestTo(BASE_URL + "/api/payments"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(PaymentFailedException.class, () -> paymentServiceClient.createPayment(requestDto, bearerToken));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw PaymentServiceUnavailableException when Payment Service returns 500 INTERNAL SERVER ERROR")
    void testCreatePaymentServerError() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        PaymentRequestDto requestDto = new PaymentRequestDto(orderId, userId, new BigDecimal("1497.00"), "INR", "MOCK");

        mockServer.expect(requestTo(BASE_URL + "/api/payments"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withServerError());

        assertThrows(PaymentServiceUnavailableException.class, () -> paymentServiceClient.createPayment(requestDto, bearerToken));
        mockServer.verify();
    }
}
