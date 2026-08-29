package com.bookecommerce.order_service.service;

import com.bookecommerce.order_service.dto.request.CreateOrderItemRequest;
import com.bookecommerce.order_service.dto.request.CreateOrderRequest;
import com.bookecommerce.order_service.dto.response.OrderItemResponse;
import com.bookecommerce.order_service.dto.response.OrderResponse;
import com.bookecommerce.order_service.entity.Order;
import com.bookecommerce.order_service.entity.OrderItem;
import com.bookecommerce.order_service.entity.OrderStatus;
import com.bookecommerce.order_service.entity.ShippingAddress;
import com.bookecommerce.order_service.exception.OrderNotFoundException;
import com.bookecommerce.order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateOrderRequest cannot be null");
        }
        if (request.userId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("items list cannot be empty");
        }

        Order order = Order.builder()
                .userId(request.userId())
                .shippingAddress(ShippingAddress.of(request.shippingAddress()))
                .status(OrderStatus.PENDING)
                .currency("INR")
                .build();

        for (CreateOrderItemRequest itemReq : request.items()) {
            if (itemReq.productId() == null) {
                throw new IllegalArgumentException("productId is required for all items");
            }
            if (itemReq.quantity() == null || itemReq.quantity() < 1) {
                throw new IllegalArgumentException("quantity must be at least 1");
            }

            BigDecimal unitPrice = (itemReq.unitPrice() != null) ? itemReq.unitPrice() : BigDecimal.ZERO;

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemReq.productId())
                    .quantity(itemReq.quantity())
                    .unitPrice(unitPrice)
                    .build();

            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse cancelOrder(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        return mapToOrderResponse(updated);
    }

    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        return mapToOrderResponse(updated);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = (order.getItems() != null)
                ? order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .collect(Collectors.toList())
                : List.of();

        String addressStr = (order.getShippingAddress() != null) ? order.getShippingAddress().getAddress() : "";

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus(),
                addressStr,
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
