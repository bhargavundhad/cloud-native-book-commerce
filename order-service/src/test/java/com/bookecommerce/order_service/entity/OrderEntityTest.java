package com.bookecommerce.order_service.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderEntityTest {

    @Test
    @DisplayName("Should correctly construct Order, add OrderItem, and calculate total")
    void testOrderAndOrderItemCalculation() {
        UUID userId = UUID.randomUUID();
        Order order = Order.builder()
                .userId(userId)
                .shippingAddress(ShippingAddress.of("123 Tech Park, Surat, Gujarat"))
                .status(OrderStatus.PENDING)
                .currency("INR")
                .build();

        OrderItem item1 = OrderItem.builder()
                .productId(UUID.randomUUID())
                .unitPrice(new BigDecimal("299.50"))
                .quantity(2)
                .build();

        OrderItem item2 = OrderItem.builder()
                .productId(UUID.randomUUID())
                .unitPrice(new BigDecimal("100.00"))
                .quantity(1)
                .build();

        order.addItem(item1);
        order.addItem(item2);

        assertEquals(new BigDecimal("599.00"), item1.getSubtotal());
        assertEquals(new BigDecimal("100.00"), item2.getSubtotal());
        assertEquals(new BigDecimal("699.00"), order.getTotalAmount());
        assertEquals("INR", order.getCurrency());
        assertEquals("123 Tech Park, Surat, Gujarat", order.getShippingAddress().getAddress());
        assertEquals(2, order.getItems().size());
        assertEquals(order, item1.getOrder());
        assertEquals(order, item2.getOrder());
    }

    @Test
    @DisplayName("Should correctly perform Money operations")
    void testMoneyOperations() {
        Money m1 = Money.of(new BigDecimal("150.00"), "INR");
        Money m2 = Money.of(new BigDecimal("250.00"), "INR");

        Money sum = m1.add(m2);
        assertEquals(new BigDecimal("400.00"), sum.getAmount());
        assertEquals("INR", sum.getCurrency());

        Money mult = m1.multiply(3);
        assertEquals(new BigDecimal("450.00"), mult.getAmount());
    }
}
