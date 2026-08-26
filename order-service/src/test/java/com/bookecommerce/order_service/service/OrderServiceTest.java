package com.bookecommerce.order_service.service;

import com.bookecommerce.order_service.dto.request.CreateOrderItemRequest;
import com.bookecommerce.order_service.dto.request.CreateOrderRequest;
import com.bookecommerce.order_service.dto.response.OrderResponse;
import com.bookecommerce.order_service.entity.Order;
import com.bookecommerce.order_service.entity.OrderStatus;
import com.bookecommerce.order_service.exception.OrderNotFoundException;
import com.bookecommerce.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private UUID productId1;
    private UUID productId2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId1 = UUID.randomUUID();
        productId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create order successfully and calculate total correctly")
    void testCreateOrderSuccess() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                List.of(
                        new CreateOrderItemRequest(productId1, 2, new BigDecimal("200.00")),
                        new CreateOrderItemRequest(productId2, 1, new BigDecimal("150.00"))
                ),
                "123 Tech Park, Surat, Gujarat"
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(userId, response.userId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(new BigDecimal("550.00"), response.totalAmount());
        assertEquals("123 Tech Park, Surat, Gujarat", response.shippingAddress());
        assertEquals(2, response.items().size());

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should reject order creation when item quantity is invalid (< 1)")
    void testCreateOrderInvalidQuantity() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                List.of(new CreateOrderItemRequest(productId1, 0, new BigDecimal("100.00"))),
                "123 Tech Park, Surat"
        );

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve order by ID successfully")
    void testGetOrderByIdSuccess() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .totalAmount(new BigDecimal("200.00"))
                .currency("INR")
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.id());
        assertEquals(userId, response.userId());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order does not exist")
    void testGetOrderByIdNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(orderId));
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Should retrieve all orders belonging to a user ID")
    void testGetOrdersByUserId() {
        Order order1 = Order.builder().id(UUID.randomUUID()).userId(userId).totalAmount(new BigDecimal("100.00")).status(OrderStatus.PENDING).build();
        Order order2 = Order.builder().id(UUID.randomUUID()).userId(userId).totalAmount(new BigDecimal("300.00")).status(OrderStatus.CREATED).build();

        when(orderRepository.findByUserId(userId)).thenReturn(List.of(order1, order2));

        List<OrderResponse> responses = orderService.getOrdersByUserId(userId);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(orderRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("Should retrieve all orders (admin)")
    void testGetAllOrders() {
        Order order1 = Order.builder().id(UUID.randomUUID()).userId(userId).totalAmount(new BigDecimal("100.00")).status(OrderStatus.PENDING).build();
        Order order2 = Order.builder().id(UUID.randomUUID()).userId(UUID.randomUUID()).totalAmount(new BigDecimal("300.00")).status(OrderStatus.CREATED).build();

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        List<OrderResponse> responses = orderService.getAllOrders();

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should cancel order successfully")
    void testCancelOrderSuccess() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .totalAmount(new BigDecimal("200.00"))
                .currency("INR")
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.status());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when cancelling an already cancelled order")
    void testCancelOrderAlreadyCancelled() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(orderId));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update order status successfully")
    void testUpdateOrderStatusSuccess() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.status());
        verify(orderRepository, times(1)).save(order);
    }
}
