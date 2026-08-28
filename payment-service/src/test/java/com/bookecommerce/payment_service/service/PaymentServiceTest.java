package com.bookecommerce.payment_service.service;

import com.bookecommerce.payment_service.dto.request.CreatePaymentRequest;
import com.bookecommerce.payment_service.dto.response.PaymentResponse;
import com.bookecommerce.payment_service.entity.Payment;
import com.bookecommerce.payment_service.entity.PaymentStatus;
import com.bookecommerce.payment_service.exception.PaymentNotFoundException;
import com.bookecommerce.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UUID orderId;
    private UUID userId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create MOCK payment successfully with status SUCCESS")
    void testCreateMockPaymentSuccess() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "ORDER",
                orderId,
                userId,
                new BigDecimal("1198.00"),
                "INR",
                "MOCK"
        );

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(paymentId);
            return saved;
        });

        PaymentResponse response = paymentService.createMockPayment(request);

        assertNotNull(response);
        assertEquals(paymentId, response.id());
        assertEquals("ORDER", response.referenceType());
        assertEquals(orderId, response.referenceId());
        assertEquals(userId, response.userId());
        assertEquals(new BigDecimal("1198.00"), response.amount());
        assertEquals("INR", response.currency());
        assertEquals("MOCK", response.paymentMethod());
        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertNotNull(response.gatewayTransactionId());
        assertTrue(response.gatewayTransactionId().startsWith("MOCK-TXN-"));

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should get payment by ID successfully")
    void testGetPaymentByIdSuccess() {
        Payment payment = Payment.builder()
                .id(paymentId)
                .referenceType("ORDER")
                .referenceId(orderId)
                .userId(userId)
                .amount(new BigDecimal("1198.00"))
                .currency("INR")
                .paymentMethod("MOCK")
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentById(paymentId);

        assertNotNull(response);
        assertEquals(paymentId, response.id());
        verify(paymentRepository, times(1)).findById(paymentId);
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when payment ID does not exist")
    void testGetPaymentByIdNotFound() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(paymentId));
        verify(paymentRepository, times(1)).findById(paymentId);
    }

    @Test
    @DisplayName("Should get payment by order ID reference successfully")
    void testGetPaymentByOrderReferenceSuccess() {
        Payment payment = Payment.builder()
                .id(paymentId)
                .referenceType("ORDER")
                .referenceId(orderId)
                .userId(userId)
                .amount(new BigDecimal("1198.00"))
                .currency("INR")
                .paymentMethod("MOCK")
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findByReferenceTypeAndReferenceId("ORDER", orderId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByOrderReference(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.referenceId());
        verify(paymentRepository, times(1)).findByReferenceTypeAndReferenceId("ORDER", orderId);
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when order payment does not exist")
    void testGetPaymentByOrderReferenceNotFound() {
        when(paymentRepository.findByReferenceTypeAndReferenceId("ORDER", orderId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentByOrderReference(orderId));
        verify(paymentRepository, times(1)).findByReferenceTypeAndReferenceId("ORDER", orderId);
    }

    @Test
    @DisplayName("Should refund successful payment")
    void testRefundPaymentSuccess() {
        Payment payment = Payment.builder()
                .id(paymentId)
                .referenceType("ORDER")
                .referenceId(orderId)
                .userId(userId)
                .amount(new BigDecimal("1198.00"))
                .currency("INR")
                .paymentMethod("MOCK")
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.refundPayment(paymentId);

        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.status());
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when refunding an already refunded payment")
    void testRefundPaymentAlreadyRefunded() {
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.REFUNDED)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class, () -> paymentService.refundPayment(paymentId));
        verify(paymentRepository, never()).save(any());
    }
}
