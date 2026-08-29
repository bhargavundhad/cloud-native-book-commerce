package com.bookecommerce.cart_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
import com.bookecommerce.cart_service.repository.CartRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    private CartService cartService;
    private UUID userId;
    private UUID productId;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository);
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        cart.setStatus(CartStatus.ACTIVE);
        lenient().when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
    void returnsExistingActiveCart() {
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertEquals(cart.getId(), cartService.getOrCreateActiveCart(userId).id());
    }

    @Test
    void addsNewProduct() {
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.addItem(userId, addRequest(2, "100.00"));

        assertEquals(1, response.items().size());
        assertEquals(2, response.items().getFirst().quantity());
    }

    @Test
    void addsSameProductByIncreasingQuantity() {
        cart.addItem(item(2, "100.00"));
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.addItem(userId, addRequest(3, "100.00"));

        assertEquals(1, cart.getItems().size());
        assertEquals(5, response.items().getFirst().quantity());
    }

    @Test
    void doesNotCreateDuplicateCartItems() {
        cart.addItem(item(1, "10.00"));
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        cartService.addItem(userId, addRequest(1, "10.00"));

        assertEquals(1, cart.getItems().size());
    }

    @Test
    void updatesItemQuantity() {
        cart.addItem(item(2, "10.00"));
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.updateItemQuantity(userId, productId,
                new UpdateCartItemQuantityRequest(7));

        assertEquals(7, response.items().getFirst().quantity());
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
