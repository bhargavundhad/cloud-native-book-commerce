package com.bookecommerce.cart_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bookecommerce.cart_service.dto.AddCartItemRequest;
import com.bookecommerce.cart_service.dto.CartResponse;
import com.bookecommerce.cart_service.dto.UpdateCartItemQuantityRequest;
import com.bookecommerce.cart_service.entity.Cart;
import com.bookecommerce.cart_service.entity.CartItem;
import com.bookecommerce.cart_service.entity.CartStatus;
import com.bookecommerce.cart_service.exception.CartItemNotFoundException;
import com.bookecommerce.cart_service.exception.CartNotFoundException;
import com.bookecommerce.cart_service.integration.inventory.InventoryServiceClient;
import com.bookecommerce.cart_service.integration.inventory.InventoryServiceIntegrationException;
import com.bookecommerce.cart_service.integration.inventory.InventoryServiceNotFoundException;
import com.bookecommerce.cart_service.integration.inventory.InventoryServiceResponse;
import com.bookecommerce.cart_service.integration.product.ProductServiceClient;
import com.bookecommerce.cart_service.integration.product.ProductServiceIntegrationException;
import com.bookecommerce.cart_service.integration.product.ProductServiceNotFoundException;
import com.bookecommerce.cart_service.integration.product.ProductServiceResponse;
import com.bookecommerce.cart_service.integration.user.UserServiceClient;
import com.bookecommerce.cart_service.integration.user.UserServiceIntegrationException;
import com.bookecommerce.cart_service.integration.user.UserServiceNotFoundException;
import com.bookecommerce.cart_service.repository.CartRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    private CartService cartService;
    private UUID userId;
    private UUID productId;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, productServiceClient, userServiceClient, inventoryServiceClient);
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        cart.setStatus(CartStatus.ACTIVE);
        lenient().when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(userServiceClient.userExists(userId)).thenReturn(true);
    }

    @Test
    void createsActiveCartWhenMissing() {
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.empty());

        CartResponse response = cartService.getOrCreateActiveCart(userId);

        assertEquals(CartStatus.ACTIVE, response.status());
        assertEquals(userId, response.userId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void rejectsCreatingActiveCartWhenUserDoesNotExist() {
        when(userServiceClient.userExists(userId)).thenReturn(false);

        UserServiceNotFoundException exception = assertThrows(UserServiceNotFoundException.class,
                () -> cartService.getOrCreateActiveCart(userId));

        assertEquals("User not found for cart operation: " + userId, exception.getMessage());
        verify(cartRepository, never()).findByUserIdAndStatus(userId, CartStatus.ACTIVE);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void doesNotModifyExistingCartWhenUserDoesNotExist() {
        cart.addItem(item(1, "10.00"));
        when(userServiceClient.userExists(userId)).thenReturn(false);

        assertThrows(UserServiceNotFoundException.class,
                () -> cartService.addItem(userId, addRequest(2, "10.00")));

        assertEquals(1, cart.getItems().size());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void rejectsRetrievingActiveCartWhenUserDoesNotExist() {
        when(userServiceClient.userExists(userId)).thenReturn(false);

        UserServiceNotFoundException exception = assertThrows(UserServiceNotFoundException.class,
                () -> cartService.getActiveCart(userId));

        assertEquals("User not found for cart operation: " + userId, exception.getMessage());
    }

    @Test
    void rejectsCartMutationWhenUserServiceUnavailable() {
        when(userServiceClient.userExists(userId))
                .thenThrow(new UserServiceIntegrationException("User service unavailable", "USER_SERVICE_UNAVAILABLE"));

        UserServiceIntegrationException exception = assertThrows(UserServiceIntegrationException.class,
                () -> cartService.addItem(userId, addRequest(1, "100.00")));

        assertEquals("User service unavailable", exception.getMessage());
    }

    @Test
    void returnsExistingActiveCart() {
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertEquals(cart.getId(), cartService.getOrCreateActiveCart(userId).id());
    }

    @Test
    void addsNewProduct() {
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(productServiceClient.getBookById(productId)).thenReturn(new ProductServiceResponse(
                productId,
                "9780134494166",
                "Clean Architecture",
                "Description",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                null,
                null));
        when(inventoryServiceClient.getInventoryByProductId(productId)).thenReturn(new InventoryServiceResponse(
                UUID.randomUUID(),
                productId,
                10,
                0,
                "AVAILABLE",
                null));

        CartResponse response = cartService.addItem(userId, addRequest(2, "100.00"));

        assertEquals(1, response.items().size());
        assertEquals(2, response.items().getFirst().quantity());
        assertEquals(new BigDecimal("100.00"), response.items().getFirst().unitPrice());
    }

    @Test
    void rejectsAddWhenInventoryIsInsufficient() {
        UUID insufficientProductId = UUID.randomUUID();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(productServiceClient.getBookById(insufficientProductId)).thenReturn(new ProductServiceResponse(
                insufficientProductId,
                "9780134494166",
                "Clean Architecture",
                "Description",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                null,
                null));
        when(inventoryServiceClient.getInventoryByProductId(insufficientProductId)).thenReturn(new InventoryServiceResponse(
                UUID.randomUUID(), insufficientProductId, 3, 1, "AVAILABLE", null));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cartService.addItem(userId, addRequest(insufficientProductId, 3, "100.00")));

        assertEquals("Insufficient stock available for product: " + insufficientProductId, exception.getMessage());
        assertEquals(0, cart.getItems().size());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void rejectsAddWhenInventoryServiceUnavailable() {
        UUID unavailableProductId = UUID.randomUUID();
        lenient().when(productServiceClient.getBookById(unavailableProductId)).thenReturn(new ProductServiceResponse(
                unavailableProductId,
                "9780134494166",
                "Clean Architecture",
                "Description",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                null,
                null));
        lenient().when(inventoryServiceClient.getInventoryByProductId(unavailableProductId))
                .thenThrow(new InventoryServiceIntegrationException("Inventory service unavailable", "INVENTORY_SERVICE_UNAVAILABLE"));

        InventoryServiceIntegrationException exception = assertThrows(InventoryServiceIntegrationException.class,
                () -> cartService.addItem(userId, addRequest(unavailableProductId, 1, "100.00")));

        assertEquals("Inventory service unavailable", exception.getMessage());
        assertEquals(0, cart.getItems().size());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void rejectsAddWhenInventoryDoesNotExist() {
        UUID missingInventoryProductId = UUID.randomUUID();
        lenient().when(productServiceClient.getBookById(missingInventoryProductId)).thenReturn(new ProductServiceResponse(
                missingInventoryProductId,
                "9780134494166",
                "Clean Architecture",
                "Description",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                null,
                null));
        lenient().when(inventoryServiceClient.getInventoryByProductId(missingInventoryProductId))
                .thenThrow(new InventoryServiceNotFoundException("Inventory not found for product: " + missingInventoryProductId, "INVENTORY_NOT_FOUND"));

        InventoryServiceNotFoundException exception = assertThrows(InventoryServiceNotFoundException.class,
                () -> cartService.addItem(userId, addRequest(missingInventoryProductId, 1, "100.00")));

        assertEquals("Inventory not found for product: " + missingInventoryProductId, exception.getMessage());
        assertEquals(0, cart.getItems().size());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addsSameProductByIncreasingQuantity() {
        cart.addItem(item(2, "100.00"));
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(productServiceClient.getBookById(productId)).thenReturn(new ProductServiceResponse(
                productId,
                "9780134494166",
                "Clean Architecture",
                "Description",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                null,
                null));
        when(inventoryServiceClient.getInventoryByProductId(productId)).thenReturn(new InventoryServiceResponse(
                UUID.randomUUID(),
                productId,
                10,
                0,
                "AVAILABLE",
                null));

        CartResponse response = cartService.addItem(userId, addRequest(3, "100.00"));

        assertEquals(1, cart.getItems().size());
        assertEquals(5, response.items().getFirst().quantity());
        assertEquals(new BigDecimal("100.00"), response.items().getFirst().unitPrice());
    }

    @Test
    void doesNotCreateDuplicateCartItems() {
        cart.addItem(item(1, "10.00"));
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(productServiceClient.getBookById(productId)).thenReturn(new ProductServiceResponse(
                productId,
                "9780134494166",
                "Clean Architecture",
                "Description",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                null,
                null));
        when(inventoryServiceClient.getInventoryByProductId(productId)).thenReturn(new InventoryServiceResponse(
                UUID.randomUUID(),
                productId,
                10,
                0,
                "AVAILABLE",
                null));

        cartService.addItem(userId, addRequest(1, "10.00"));

        assertEquals(1, cart.getItems().size());
    }

    @Test
    void updatesItemQuantity() {
        cart.addItem(item(2, "10.00"));
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(inventoryServiceClient.getInventoryByProductId(productId)).thenReturn(new InventoryServiceResponse(
                UUID.randomUUID(), productId, 20, 0, "AVAILABLE", null));

        CartResponse response = cartService.updateItemQuantity(userId, productId,
                new UpdateCartItemQuantityRequest(7));

        assertEquals(7, response.items().getFirst().quantity());
    }

    @Test
    void rejectsUpdateWhenFinalQuantityExceedsAvailableInventory() {
        cart.addItem(item(2, "10.00"));
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(inventoryServiceClient.getInventoryByProductId(productId)).thenReturn(new InventoryServiceResponse(
                UUID.randomUUID(), productId, 5, 1, "AVAILABLE", null));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cartService.updateItemQuantity(userId, productId, new UpdateCartItemQuantityRequest(5)));

        assertEquals("Insufficient stock available for product: " + productId, exception.getMessage());
        assertEquals(2, cart.getItems().getFirst().getQuantity());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void rejectsZeroQuantity() {
        assertEquals("Quantity must be greater than zero",
            assertThrows(IllegalArgumentException.class, () -> cartService.addItem(userId, addRequest(0, "10.00")))
                .getMessage());
    }

    @Test
    void rejectsNegativeQuantity() {
        assertEquals("Quantity must be greater than zero",
            assertThrows(IllegalArgumentException.class,
                () -> cartService.updateItemQuantity(userId, productId, new UpdateCartItemQuantityRequest(-1)))
                .getMessage());
    }

    @Test
    void removesItem() {
        CartItem item = item(2, "10.00");
        cart.addItem(item);
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        cartService.removeItem(userId, productId);

        assertEquals(0, cart.getItems().size());
    }

    @Test
    void rejectsNegativePrice() {
        assertEquals("Unit price must not be negative",
            assertThrows(IllegalArgumentException.class, () -> cartService.addItem(userId, addRequest(1, "-1.00")))
                .getMessage());
    }

    @Test
    void clearsCartAndKeepsItActive() {
        cart.addItem(item(2, "10.00"));
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.clearCart(userId);

        assertEquals(0, response.items().size());
        assertEquals(CartStatus.ACTIVE, response.status());
    }

    @Test
    void calculatesCartTotalWithBigDecimal() {
        cart.addItem(item(2, "100.00"));
        CartItem second = new CartItem();
        second.setProductId(UUID.randomUUID());
        second.setQuantity(1);
        second.setUnitPrice(new BigDecimal("200.00"));
        cart.addItem(second);
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertEquals(new BigDecimal("400.00"), cartService.calculateTotal(userId));
    }

    @Test
    void productNotFoundDoesNotCreateCartItem() {
        UUID missingProductId = UUID.randomUUID();
        lenient().when(productServiceClient.getBookById(missingProductId))
                .thenThrow(new ProductServiceNotFoundException("Product not found: " + missingProductId, "PRODUCT_NOT_FOUND"));

        ProductServiceNotFoundException exception = assertThrows(ProductServiceNotFoundException.class,
                () -> cartService.addItem(userId, addRequest(missingProductId, 1, "100.00")));
        assertEquals("Product not found: " + missingProductId, exception.getMessage());
        assertEquals(0, cart.getItems().size());
    }

    @Test
    void productServiceUnavailableDoesNotMutateCart() {
        UUID unavailableProductId = UUID.randomUUID();
        lenient().when(productServiceClient.getBookById(unavailableProductId))
                .thenThrow(new ProductServiceIntegrationException("Product service unavailable", "PRODUCT_SERVICE_UNAVAILABLE"));

        ProductServiceIntegrationException exception = assertThrows(ProductServiceIntegrationException.class,
                () -> cartService.addItem(userId, addRequest(unavailableProductId, 1, "100.00")));
        assertEquals("Product service unavailable", exception.getMessage());
        assertEquals(0, cart.getItems().size());
    }

    @Test
    void rejectsMissingCart() {
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.empty());

        assertEquals("Active cart not found for user: " + userId,
            assertThrows(CartNotFoundException.class, () -> cartService.getActiveCart(userId)).getMessage());
    }

    @Test
    void rejectsMissingCartItem() {
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertEquals("Cart item not found for product: " + productId,
            assertThrows(CartItemNotFoundException.class, () -> cartService.removeItem(userId, productId))
                .getMessage());
    }

    @Test
    void looksUpExistingActiveCartBeforeCreatingAnother() {
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        cartService.getOrCreateActiveCart(userId);

        verify(cartRepository).findByUserIdAndStatus(userId, CartStatus.ACTIVE);
    }

    private AddCartItemRequest addRequest(int quantity, String price) {
        return addRequest(productId, quantity, price);
    }

    private AddCartItemRequest addRequest(UUID productId, int quantity, String price) {
        return new AddCartItemRequest(productId, quantity, new BigDecimal(price));
    }

    private CartItem item(int quantity, String price) {
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(price));
        return item;
    }
}
