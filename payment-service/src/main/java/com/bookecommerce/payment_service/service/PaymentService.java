package com.bookecommerce.payment_service.service;

import com.bookecommerce.payment_service.dto.request.CreatePaymentRequest;
import com.bookecommerce.payment_service.dto.response.PaymentResponse;
import com.bookecommerce.payment_service.entity.Payment;
import com.bookecommerce.payment_service.entity.PaymentStatus;
import com.bookecommerce.payment_service.exception.PaymentNotFoundException;
import com.bookecommerce.payment_service.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse createMockPayment(CreatePaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreatePaymentRequest cannot be null");
        }

        UUID userId = request.userId() != null ? request.userId() : request.referenceId();
        String gatewayTxnId = "MOCK-TXN-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .referenceType(request.referenceType())
                .referenceId(request.referenceId())
                .userId(userId)
                .amount(request.amount())
                .currency(request.currency())
                .paymentMethod(request.paymentMethod())
                .gatewayTransactionId(gatewayTxnId)
                .status(PaymentStatus.SUCCESS)
                .paidAt(now)
                .build();

        Payment saved = paymentRepository.save(payment);
        return mapToPaymentResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Payment id cannot be null");
        }
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderReference(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }
        Payment payment = paymentRepository.findByReferenceTypeAndReferenceId("ORDER", orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order id: " + orderId));
        return mapToPaymentResponse(payment);
    }

    public PaymentResponse refundPayment(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Payment id cannot be null");
        }
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Payment is already refunded");
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only successful payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment updated = paymentRepository.save(payment);
        return mapToPaymentResponse(updated);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReferenceType(),
                payment.getReferenceId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getGatewayTransactionId(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
