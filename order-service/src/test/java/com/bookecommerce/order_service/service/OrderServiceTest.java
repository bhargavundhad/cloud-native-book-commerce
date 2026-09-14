package com.bookecommerce.order_service.service;

import com.bookecommerce.order_service.client.CartServiceClient;
import com.bookecommerce.order_service.client.ProductServiceClient;
import com.bookecommerce.order_service.client.UserServiceClient;
import com.bookecommerce.order_service.client.dto.CartItemResponseDto;
import com.bookecommerce.order_service.client.dto.CartResponseDto;
import com.bookecommerce.order_service.client.dto.ProductResponseDto;
import com.bookecommerce.order_service.client.dto.UserResponseDto;
import com.bookecommerce.order_service.dto.request.CreateOrderRequest;
import com.bookecommerce.order_service.dto.response.OrderResponse;
import com.bookecommerce.order_service.entity.Order;
import com.bookecommerce.order_service.entity.OrderStatus;
import com.bookecommerce.order_service.exception.CartEmptyException;
import com.bookecommerce.order_service.exception.CartNotActiveException;
import com.bookecommerce.order_service.exception.CartNotFoundException;
import com.bookecommerce.order_service.exception.CartServiceUnavailableException;
import com.bookecommerce.order_service.exception.OrderNotFoundException;
import com.bookecommerce.order_service.exception.ProductNotFoundException;
import com.bookecommerce.order_service.exception.ProductServiceUnavailableException;
import com.bookecommerce.order_service.exception.UserForbiddenException;
import com.bookecommerce.order_service.exception.UserNotFoundException;
import com.bookecommerce.order_service.exception.UserServiceUnavailableException;
import com.bookecommerce.order_service.repository.OrderRepository;
import com.bookecommerce.order_service.security.JwtTokenValidator;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private JwtTokenValidator jwtTokenValidator;

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
    @DisplayName("Should create order successfully using active cart after validating single product with Product Service")
    void testCreateOrderSuccessSingleProductValidated() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                null,
                "123 Tech Park, Surat, Gujarat"
        );

        UserResponseDto validUser = new UserResponseDto(userId, "John", "Doe", "john@example.com", "9876543210", "CUSTOMER", true);
        when(userServiceClient.verifyUserExists(eq(userId), any())).thenReturn(validUser);

        CartItemResponseDto cartItem = new CartItemResponseDto(UUID.randomUUID(), productId1, 2, new BigDecimal("499.00"));
        CartResponseDto activeCart = new CartResponseDto(UUID.randomUUID(), userId, "ACTIVE", List.of(cartItem), new BigDecimal("998.00"));
        when(cartServiceClient.getActiveCart(eq(userId), any())).thenReturn(activeCart);

        ProductResponseDto validProduct = new ProductResponseDto(productId1, "9781234567890", "Clean Code", "Tech book", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("499.00"));
        when(productServiceClient.getProduct(eq(productId1), any())).thenReturn(validProduct);

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
        assertEquals(new BigDecimal("998.00"), response.totalAmount());
        assertEquals("123 Tech Park, Surat, Gujarat", response.shippingAddress());
        assertEquals(1, response.items().size());
        assertEquals(productId1, response.items().get(0).productId());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(new BigDecimal("499.00"), response.items().get(0).unitPrice());

        verify(cartServiceClient, times(1)).getActiveCart(eq(userId), any());
        verify(productServiceClient, times(1)).getProduct(eq(productId1), any());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should create order successfully using active cart after validating multiple products with Product Service")
    void testCreateOrderSuccessMultipleProductsValidated() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                null,
                "123 Tech Park, Surat, Gujarat"
        );

        UserResponseDto validUser = new UserResponseDto(userId, "John", "Doe", "john@example.com", "9876543210", "CUSTOMER", true);
        when(userServiceClient.verifyUserExists(eq(userId), any())).thenReturn(validUser);

        CartItemResponseDto item1 = new CartItemResponseDto(UUID.randomUUID(), productId1, 2, new BigDecimal("499.00"));
        CartItemResponseDto item2 = new CartItemResponseDto(UUID.randomUUID(), productId2, 1, new BigDecimal("799.00"));
        CartResponseDto activeCart = new CartResponseDto(UUID.randomUUID(), userId, "ACTIVE", List.of(item1, item2), new BigDecimal("1797.00"));
        when(cartServiceClient.getActiveCart(eq(userId), any())).thenReturn(activeCart);

        ProductResponseDto product1 = new ProductResponseDto(productId1, "9781234567890", "Clean Code", "Tech", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("499.00"));
        ProductResponseDto product2 = new ProductResponseDto(productId2, "9780987654321", "Refactoring", "Tech", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("799.00"));
        when(productServiceClient.getProduct(eq(productId1), any())).thenReturn(product1);
        when(productServiceClient.getProduct(eq(productId2), any())).thenReturn(product2);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("1797.00"), response.totalAmount());
        assertEquals(2, response.items().size());

        verify(productServiceClient, times(1)).getProduct(eq(productId1), any());
        verify(productServiceClient, times(1)).getProduct(eq(productId2), any());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should reject order creation and NOT persist when cart product does not exist in Product Service")
    void testCreateOrderProductNotFound() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                null,
                "123 Tech Park, Surat"
        );

        UserResponseDto validUser = new UserResponseDto(userId, "John", "Doe", "john@example.com", "9876543210", "CUSTOMER", true);
        when(userServiceClient.verifyUserExists(eq(userId), any())).thenReturn(validUser);

        CartItemResponseDto item = new CartItemResponseDto(UUID.randomUUID(), productId1, 1, new BigDecimal("499.00"));
        CartResponseDto activeCart = new CartResponseDto(UUID.randomUUID(), userId, "ACTIVE", List.of(item), new BigDecimal("499.00"));
        when(cartServiceClient.getActiveCart(eq(userId), any())).thenReturn(activeCart);

        doThrow(new ProductNotFoundException(productId1)).when(productServiceClient).getProduct(eq(productId1), any());

        assertThrows(ProductNotFoundException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject order creation and NOT persist when Product Service is unavailable")
    void testCreateOrderProductServiceUnavailable() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                null,
                "123 Tech Park, Surat"
        );

        UserResponseDto validUser = new UserResponseDto(userId, "John", "Doe", "john@example.com", "9876543210", "CUSTOMER", true);
        when(userServiceClient.verifyUserExists(eq(userId), any())).thenReturn(validUser);

        CartItemResponseDto item = new CartItemResponseDto(UUID.randomUUID(), productId1, 1, new BigDecimal("499.00"));
        CartResponseDto activeCart = new CartResponseDto(UUID.randomUUID(), userId, "ACTIVE", List.of(item), new BigDecimal("499.00"));
        when(cartServiceClient.getActiveCart(eq(userId), any())).thenReturn(activeCart);

        doThrow(new ProductServiceUnavailableException("Product Service is unreachable")).when(productServiceClient).getProduct(eq(productId1), any());

        assertThrows(ProductServiceUnavailableException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject order creation and NOT persist when 1 product is valid and 1 product is invalid")
    void testCreateOrderOneValidOneInvalidProduct() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                null,
                "123 Tech Park, Surat"
        );

        UserResponseDto validUser = new UserResponseDto(userId, "John", "Doe", "john@example.com", "9876543210", "CUSTOMER", true);
        when(userServiceClient.verifyUserExists(eq(userId), any())).thenReturn(validUser);

        CartItemResponseDto item1 = new CartItemResponseDto(UUID.randomUUID(), productId1, 1, new BigDecimal("499.00"));
        CartItemResponseDto item2 = new CartItemResponseDto(UUID.randomUUID(), productId2, 1, new BigDecimal("799.00"));
        CartResponseDto activeCart = new CartResponseDto(UUID.randomUUID(), userId, "ACTIVE", List.of(item1, item2), new BigDecimal("1298.00"));
        when(cartServiceClient.getActiveCart(eq(userId), any())).thenReturn(activeCart);

        ProductResponseDto product1 = new ProductResponseDto(productId1, "9781234567890", "Clean Code", "Tech", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("499.00"));
        when(productServiceClient.getProduct(eq(productId1), any())).thenReturn(product1);
        doThrow(new ProductNotFoundException(productId2)).when(productServiceClient).getProduct(eq(productId2), any());

        assertThrows(ProductNotFoundException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject order creation and NOT persist when active cart is empty")
    void testCreateOrderCartEmpty() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                null,
                "123 Tech Park, Surat"
        );

        UserResponseDto validUser = new UserResponseDto(userId, "John", "Doe", "john@example.com", "9876543210", "CUSTOMER", true);
        when(userServiceClient.verifyUserExists(eq(userId), any())).thenReturn(validUser);

        CartResponseDto emptyCart = new CartResponseDto(UUID.randomUUID(), userId, "ACTIVE", List.of(), BigDecimal.ZERO);
        when(cartServiceClient.getActiveCart(eq(userId), any())).thenReturn(emptyCart);

        assertThrows(CartEmptyException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject order creation and NOT persist when cart is not active")
    void testCreateOrderCartNotActive() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                null,
                "123 Tech Park, Surat"
        );

        UserResponseDto validUser = new UserResponseDto(userId, "John", "Doe", "john@example.com", "9876543210", "CUSTOMER", true);
        when(userServiceClient.verifyUserExists(eq(userId), any())).thenReturn(validUser);

        CartItemResponseDto item = new CartItemResponseDto(UUID.randomUUID(), productId1, 1, new BigDecimal("100.00"));
        CartResponseDto completedCart = new CartResponseDto(UUID.randomUUID(), userId, "COMPLETED", List.of(item), new BigDecimal("100.00"));
        when(cartServiceClient.getActiveCart(eq(userId), any())).thenReturn(completedCart);

        assertThrows(CartNotActiveException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject order creation and NOT persist when active cart does not exist")
    void testCreateOrderCartNotFound() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                null,
                "123 Tech Park, Surat"
        );

        UserResponseDto validUser = new UserResponseDto(userId, "John", "Doe", "john@example.com", "9876543210", "CUSTOMER", true);
        when(userServiceClient.verifyUserExists(eq(userId), any())).thenReturn(validUser);

        doThrow(new CartNotFoundException(userId)).when(cartServiceClient).getActiveCart(eq(userId), any());

        assertThrows(CartNotFoundException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject order creation and NOT persist when Cart Service is unavailable")
    void testCreateOrderCartServiceUnavailable() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                null,
                "123 Tech Park, Surat"
        );

        UserResponseDto validUser = new UserResponseDto(userId, "John", "Doe", "john@example.com", "9876543210", "CUSTOMER", true);
        when(userServiceClient.verifyUserExists(eq(userId), any())).thenReturn(validUser);

        doThrow(new CartServiceUnavailableException("Cart Service is unreachable")).when(cartServiceClient).getActiveCart(eq(userId), any());

        assertThrows(CartServiceUnavailableException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject order creation and NOT persist when user is unknown")
    void testCreateOrderUnknownUser() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                null,
                "123 Tech Park, Surat"
        );

        doThrow(new UserNotFoundException(userId)).when(userServiceClient).verifyUserExists(eq(userId), any());

        assertThrows(UserNotFoundException.class, () -> orderService.createOrder(request));
        verify(userServiceClient, times(1)).verifyUserExists(eq(userId), any());
        verify(cartServiceClient, never()).getActiveCart(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject order creation and NOT persist when User Service is unavailable")
    void testCreateOrderUserServiceUnavailable() {
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                null,
                "123 Tech Park, Surat"
        );

        doThrow(new UserServiceUnavailableException("User Service is unreachable")).when(userServiceClient).verifyUserExists(eq(userId), any());

        assertThrows(UserServiceUnavailableException.class, () -> orderService.createOrder(request));
        verify(userServiceClient, times(1)).verifyUserExists(eq(userId), any());
        verify(cartServiceClient, never()).getActiveCart(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject order creation and NOT persist when User A attempts to create order for User B (User ID Tampering)")
    void testCreateOrderUserImpersonationRejected() {
        UUID userBId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                userBId,
                null,
                "123 Tech Park, Surat"
        );

        doThrow(new UserForbiddenException("User is not authorized to create an order on behalf of another user"))
                .when(userServiceClient).verifyUserExists(eq(userBId), any());

        assertThrows(UserForbiddenException.class, () -> orderService.createOrder(request));
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
