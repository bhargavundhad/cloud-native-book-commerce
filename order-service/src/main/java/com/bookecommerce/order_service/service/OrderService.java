package com.bookecommerce.order_service.service;

import com.bookecommerce.order_service.client.CartServiceClient;
import com.bookecommerce.order_service.client.InventoryServiceClient;
import com.bookecommerce.order_service.client.PaymentServiceClient;
import com.bookecommerce.order_service.client.ProductServiceClient;
import com.bookecommerce.order_service.client.UserServiceClient;
import com.bookecommerce.order_service.client.dto.CartItemResponseDto;
import com.bookecommerce.order_service.client.dto.CartResponseDto;
import com.bookecommerce.order_service.client.dto.InventoryResponseDto;
import com.bookecommerce.order_service.client.dto.PaymentRequestDto;
import com.bookecommerce.order_service.dto.request.CreateOrderItemRequest;
import com.bookecommerce.order_service.dto.request.CreateOrderRequest;
import com.bookecommerce.order_service.dto.response.OrderItemResponse;
import com.bookecommerce.order_service.dto.response.OrderResponse;
import com.bookecommerce.order_service.entity.Order;
import com.bookecommerce.order_service.entity.OrderItem;
import com.bookecommerce.order_service.entity.OrderStatus;
import com.bookecommerce.order_service.entity.ShippingAddress;
import com.bookecommerce.order_service.exception.CartEmptyException;
import com.bookecommerce.order_service.exception.CartNotActiveException;
import com.bookecommerce.order_service.exception.CartNotFoundException;
import com.bookecommerce.order_service.exception.OrderNotFoundException;
import com.bookecommerce.order_service.exception.UserForbiddenException;
import com.bookecommerce.order_service.repository.OrderRepository;
import com.bookecommerce.order_service.security.JwtTokenValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final CartServiceClient cartServiceClient;
    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final JwtTokenValidator jwtTokenValidator;

    public OrderService(OrderRepository orderRepository,
                        UserServiceClient userServiceClient,
                        CartServiceClient cartServiceClient,
                        ProductServiceClient productServiceClient,
                        InventoryServiceClient inventoryServiceClient,
                        PaymentServiceClient paymentServiceClient,
                        JwtTokenValidator jwtTokenValidator) {
        this.orderRepository = orderRepository;
        this.userServiceClient = userServiceClient;
        this.cartServiceClient = cartServiceClient;
        this.productServiceClient = productServiceClient;
        this.inventoryServiceClient = inventoryServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateOrderRequest cannot be null");
        }
        if (request.userId() == null) {
            throw new IllegalArgumentException("userId is required");
        }

        String authHeader = getIncomingAuthorizationHeader();

        // Validate JWT identity matching if Authorization header is provided
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            UUID authenticatedUserId = jwtTokenValidator.extractUserIdFromHeader(authHeader);
            String role = jwtTokenValidator.extractRoleFromHeader(authHeader);

            if (!request.userId().equals(authenticatedUserId) && !"ADMIN".equalsIgnoreCase(role)) {
                throw new UserForbiddenException("User is not authorized to create an order on behalf of another user");
            }
        }

        // Verify user existence synchronously via User Service
        userServiceClient.verifyUserExists(request.userId(), authHeader);

        // Fetch user's active cart from Cart Service
        CartResponseDto cart = cartServiceClient.getActiveCart(request.userId(), authHeader);
        if (cart == null) {
            throw new CartNotFoundException(request.userId());
        }

        if (cart.status() == null || !"ACTIVE".equalsIgnoreCase(cart.status())) {
            throw new CartNotActiveException("Cart is not active for user: " + request.userId());
        }

        List<CartItemResponseDto> cartItems = cart.items();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new CartEmptyException("Cannot create order from an empty cart");
        }

        // Validate every cart item's product against Product Service
        for (CartItemResponseDto item : cartItems) {
            if (item.productId() == null) {
                throw new IllegalArgumentException("productId is required for all cart items");
            }
            if (item.quantity() == null || item.quantity() < 1) {
                throw new IllegalArgumentException("quantity must be at least 1");
            }
            // Product Service synchronous validation
            productServiceClient.getProduct(item.productId(), authHeader);
        }

        // Reserve Inventory for each cart item with multi-item rollback protection
        List<UUID> acquiredReservationIds = new ArrayList<>();
        try {
            for (CartItemResponseDto item : cartItems) {
                InventoryResponseDto reservation = inventoryServiceClient.reserveStock(item.productId(), item.quantity(), authHeader);
                if (reservation != null && reservation.reservationId() != null) {
                    acquiredReservationIds.add(reservation.reservationId());
                }
            }
        } catch (RuntimeException ex) {
            for (UUID reservationId : acquiredReservationIds) {
                try {
                    inventoryServiceClient.releaseReservation(reservationId, authHeader);
                } catch (Exception releaseEx) {
                    System.err.println("Failed to release reservation " + reservationId + ": " + releaseEx.getMessage());
                }
            }
            throw ex;
        }

        Order order = Order.builder()
                .userId(request.userId())
                .shippingAddress(ShippingAddress.of(request.shippingAddress()))
                .status(OrderStatus.PENDING)
                .currency("INR")
                .build();

        for (CartItemResponseDto item : cartItems) {
            BigDecimal unitPrice = (item.unitPrice() != null) ? item.unitPrice() : BigDecimal.ZERO;

            OrderItem orderItem = OrderItem.builder()
                    .productId(item.productId())
                    .quantity(item.quantity())
                    .unitPrice(unitPrice)
                    .build();

            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // Process Payment with Payment Service synchronously
        PaymentRequestDto paymentRequest = new PaymentRequestDto(
                "ORDER",
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmount(),
                savedOrder.getCurrency(),
                "MOCK"
        );

        try {
            paymentServiceClient.createPayment(paymentRequest, authHeader);
        } catch (RuntimeException paymentEx) {
            for (UUID reservationId : acquiredReservationIds) {
                try {
                    inventoryServiceClient.releaseReservation(reservationId, authHeader);
                } catch (Exception releaseEx) {
                    System.err.println("Failed to release reservation " + reservationId + ": " + releaseEx.getMessage());
                }
            }
            savedOrder.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(savedOrder);
            throw paymentEx;
        }

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

    private String getIncomingAuthorizationHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest() != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader(HttpHeaders.AUTHORIZATION);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
