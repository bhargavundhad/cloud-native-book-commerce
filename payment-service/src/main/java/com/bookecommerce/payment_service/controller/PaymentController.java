package com.bookecommerce.payment_service.controller;

import com.bookecommerce.payment_service.dto.request.CreatePaymentRequest;
import com.bookecommerce.payment_service.dto.response.PaymentResponse;
import com.bookecommerce.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createMockPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createMockPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable UUID id) {
        return paymentService.getPaymentById(id);
    }

    @GetMapping("/order/{orderId}")
    public PaymentResponse getPaymentByOrderReference(@PathVariable UUID orderId) {
        return paymentService.getPaymentByOrderReference(orderId);
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refundPayment(@PathVariable UUID id) {
        return paymentService.refundPayment(id);
    }
}
