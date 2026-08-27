package com.bookecommerce.order_service.controller;

import com.bookecommerce.order_service.dto.request.CreateOrderItemRequest;
import com.bookecommerce.order_service.dto.request.CreateOrderRequest;
import com.bookecommerce.order_service.dto.request.UpdateOrderStatusRequest;
import com.bookecommerce.order_service.dto.response.OrderResponse;
import com.bookecommerce.order_service.entity.OrderStatus;
import com.bookecommerce.order_service.exception.GlobalExceptionHandler;
import com.bookecommerce.order_service.exception.OrderNotFoundException;
import com.bookecommerce.order_service.service.OrderService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private UUID userId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/orders should create order and return HTTP 201 CREATED")
    void testCreateOrderEndpoint() throws Exception {
        UUID productId = UUID.randomUUID();
        String jsonPayload = """
                {
                    "userId": "%s",
                    "items": [
                        {
                            "productId": "%s",
                            "quantity": 2,
                            "unitPrice": 200.00
                        }
                    ],
                    "shippingAddress": "123 Tech Park"
                }
                """.formatted(userId, productId);

        OrderResponse mockResponse = new OrderResponse(
                orderId,
                userId,
                new BigDecimal("400.00"),
                "INR",
                OrderStatus.PENDING,
                "123 Tech Park",
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(400.00));
    }

    @Test
    @DisplayName("GET /api/orders should return all orders (admin)")
    void testGetAllOrdersEndpoint() throws Exception {
        OrderResponse mockResponse = new OrderResponse(
                orderId,
                userId,
                new BigDecimal("400.00"),
                "INR",
                OrderStatus.PENDING,
                "123 Tech Park",
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.getAllOrders()).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId.toString()));
    }

    @Test
    @DisplayName("GET /api/orders/my should return user's orders")
    void testGetMyOrdersEndpoint() throws Exception {
        OrderResponse mockResponse = new OrderResponse(
                orderId,
                userId,
                new BigDecimal("400.00"),
                "INR",
                OrderStatus.PENDING,
                "123 Tech Park",
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.getOrdersByUserId(userId)).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/orders/my").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId.toString()));
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} should return order and HTTP 200 OK")
    void testGetOrderByIdEndpointSuccess() throws Exception {
        OrderResponse mockResponse = new OrderResponse(
                orderId,
                userId,
                new BigDecimal("400.00"),
                "INR",
                OrderStatus.PENDING,
                "123 Tech Park",
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.getOrderById(orderId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} should return HTTP 404 NOT FOUND when order does not exist")
    void testGetOrderByIdEndpointNotFound() throws Exception {
        when(orderService.getOrderById(orderId)).thenThrow(new OrderNotFoundException(orderId));

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/orders/{orderId}/cancel should cancel order")
    void testCancelOrderEndpoint() throws Exception {
        OrderResponse mockResponse = new OrderResponse(
                orderId,
                userId,
                new BigDecimal("400.00"),
                "INR",
                OrderStatus.CANCELLED,
                "123 Tech Park",
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.cancelOrder(orderId)).thenReturn(mockResponse);

        mockMvc.perform(put("/api/orders/" + orderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("PUT /api/orders/{orderId}/status should update order status")
    void testUpdateOrderStatusEndpoint() throws Exception {
        String jsonPayload = """
                {
                    "status": "CONFIRMED"
                }
                """;

        OrderResponse mockResponse = new OrderResponse(
                orderId,
                userId,
                new BigDecimal("400.00"),
                "INR",
                OrderStatus.CONFIRMED,
                "123 Tech Park",
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.updateOrderStatus(eq(orderId), eq(OrderStatus.CONFIRMED))).thenReturn(mockResponse);

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
