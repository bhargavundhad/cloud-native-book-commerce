package com.bookecommerce.cart_service.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookecommerce.cart_service.dto.AddCartItemRequest;
import com.bookecommerce.cart_service.dto.CartItemResponse;
import com.bookecommerce.cart_service.dto.CartResponse;
import com.bookecommerce.cart_service.dto.UpdateCartItemQuantityRequest;
import com.bookecommerce.cart_service.entity.Cart;
import com.bookecommerce.cart_service.entity.CartItem;
import com.bookecommerce.cart_service.entity.CartStatus;
import com.bookecommerce.cart_service.exception.CartItemNotFoundException;
import com.bookecommerce.cart_service.exception.CartNotFoundException;
import com.bookecommerce.cart_service.repository.CartRepository;

@Service
public class CartService {

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Transactional
    public CartResponse getOrCreateActiveCart(UUID userId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> createActiveCart(userId));
        return toResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getActiveCart(UUID userId) {
        return toResponse(findActiveCart(userId));
    }

    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        validateAddItemRequest(request);
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> createActiveCart(userId));

        CartItem item = cart.getItems().stream()
                .filter(existingItem -> existingItem.getProductId().equals(request.productId()))
                .findFirst()
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setProductId(request.productId());
                    newItem.setQuantity(0);
                    newItem.setUnitPrice(request.unitPrice());
                    cart.addItem(newItem);
                    return newItem;
                });
        item.setQuantity(item.getQuantity() + request.quantity());
        item.setUnitPrice(request.unitPrice());

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID userId, UUID productId,
            UpdateCartItemQuantityRequest request) {
        validateQuantity(request.quantity());
        Cart cart = findActiveCart(userId);
        CartItem item = findItem(cart, productId);
        item.setQuantity(request.quantity());
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID productId) {
        Cart cart = findActiveCart(userId);
        CartItem item = findItem(cart, productId);
        cart.removeItem(item);
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse clearCart(UUID userId) {
        Cart cart = findActiveCart(userId);
        cart.getItems().clear();
        return toResponse(cartRepository.save(cart));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotal(UUID userId) {
        return calculateTotal(findActiveCart(userId));
    }

    private Cart createActiveCart(UUID userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setStatus(CartStatus.ACTIVE);
        return cartRepository.save(cart);
    }

    private void validateAddItemRequest(AddCartItemRequest request) {
        if (request == null || request.productId() == null || request.unitPrice() == null) {
            throw new IllegalArgumentException("Product ID and unit price are required");
        }
        validateQuantity(request.quantity());
        if (request.unitPrice().signum() < 0) {
            throw new IllegalArgumentException("Unit price must not be negative");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    private Cart findActiveCart(UUID userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException("Active cart not found for user: " + userId));
    }

    private CartItem findItem(Cart cart, UUID productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found for product: " + productId));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> new CartItemResponse(item.getId(), item.getProductId(), item.getQuantity(),
                        item.getUnitPrice()))
                .toList();
        return new CartResponse(cart.getId(), cart.getUserId(), cart.getStatus(), items, calculateTotal(cart));
    }

    private BigDecimal calculateTotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
