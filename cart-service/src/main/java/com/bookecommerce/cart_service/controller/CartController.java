package com.bookecommerce.cart_service.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookecommerce.cart_service.dto.AddCartItemRequest;
import com.bookecommerce.cart_service.dto.CartResponse;
import com.bookecommerce.cart_service.dto.UpdateCartItemQuantityRequest;
import com.bookecommerce.cart_service.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/carts/{userId}/active")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<CartResponse> getOrCreateActiveCart(@PathVariable UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.getOrCreateActiveCart(userId));
    }

    @GetMapping
    public CartResponse getActiveCart(@PathVariable UUID userId) {
        return cartService.getActiveCart(userId);
    }

    @PostMapping("/items")
    public CartResponse addItem(@PathVariable UUID userId, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(userId, request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateItemQuantity(@PathVariable UUID userId, @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        return cartService.updateItemQuantity(userId, productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable UUID userId, @PathVariable UUID productId) {
        return cartService.removeItem(userId, productId);
    }

    @DeleteMapping
    public CartResponse clearCart(@PathVariable UUID userId) {
        return cartService.clearCart(userId);
    }

    @GetMapping("/total")
    public BigDecimal calculateTotal(@PathVariable UUID userId) {
        return cartService.calculateTotal(userId);
    }
}
