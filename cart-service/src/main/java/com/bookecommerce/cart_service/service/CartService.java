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
import com.bookecommerce.cart_service.integration.inventory.InventoryInsufficientStockException;
import com.bookecommerce.cart_service.integration.inventory.InventoryServiceClient;
import com.bookecommerce.cart_service.integration.inventory.InventoryServiceIntegrationException;
import com.bookecommerce.cart_service.integration.inventory.InventoryServiceNotFoundException;
import com.bookecommerce.cart_service.integration.inventory.InventoryServiceResponse;
import com.bookecommerce.cart_service.integration.product.ProductServiceClient;
import com.bookecommerce.cart_service.integration.user.UserServiceClient;
import com.bookecommerce.cart_service.integration.user.UserServiceIntegrationException;
import com.bookecommerce.cart_service.integration.user.UserServiceNotFoundException;
import com.bookecommerce.cart_service.repository.CartRepository;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;
    private final InventoryServiceClient inventoryServiceClient;

    public CartService(CartRepository cartRepository, ProductServiceClient productServiceClient,
            UserServiceClient userServiceClient, InventoryServiceClient inventoryServiceClient) {
        this.cartRepository = cartRepository;
        this.productServiceClient = productServiceClient;
        this.userServiceClient = userServiceClient;
        this.inventoryServiceClient = inventoryServiceClient;
    }

    @Transactional
    public CartResponse getOrCreateActiveCart(UUID userId) {
        ensureUserExists(userId);
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> createActiveCart(userId));
        return toResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getActiveCart(UUID userId) {
        ensureUserExists(userId);
        return toResponse(findActiveCart(userId));
    }

    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        validateAddItemRequest(request);
        ensureUserExists(userId);

        var product = productServiceClient.getBookById(request.productId());
        int requestedQuantity = request.quantity();
        int availableStock = getAvailableStock(request.productId());

        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE).orElse(null);
        if (cart == null) {
            validateAvailableStock(request.productId(), requestedQuantity, availableStock);
            cart = createActiveCart(userId);
        } else {
            CartItem item = cart.getItems().stream()
                    .filter(existingItem -> existingItem.getProductId().equals(request.productId()))
                    .findFirst()
                    .orElse(null);
            int finalQuantity = item == null ? requestedQuantity : item.getQuantity() + requestedQuantity;
            validateAvailableStock(request.productId(), finalQuantity, availableStock);
            if (item == null) {
                item = new CartItem();
                item.setProductId(request.productId());
                item.setQuantity(0);
                item.setUnitPrice(product.price());
                cart.addItem(item);
            }
            item.setQuantity(finalQuantity);
            item.setUnitPrice(product.price());
            return toResponse(cartRepository.save(cart));
        }

        CartItem item = cart.getItems().stream()
                .filter(existingItem -> existingItem.getProductId().equals(request.productId()))
                .findFirst()
                .orElse(null);
        if (item == null) {
            item = new CartItem();
            item.setProductId(request.productId());
            item.setQuantity(0);
            item.setUnitPrice(product.price());
            cart.addItem(item);
        }
        item.setQuantity(requestedQuantity);
        item.setUnitPrice(product.price());

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID userId, UUID productId,
            UpdateCartItemQuantityRequest request) {
        validateQuantity(request.quantity());
        ensureUserExists(userId);
        Cart cart = findActiveCart(userId);
        CartItem item = findItem(cart, productId);
        int availableStock = getAvailableStock(productId);
        validateAvailableStock(productId, request.quantity(), availableStock);
        item.setQuantity(request.quantity());
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID productId) {
        ensureUserExists(userId);
        Cart cart = findActiveCart(userId);
        CartItem item = findItem(cart, productId);
        cart.removeItem(item);
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse clearCart(UUID userId) {
        ensureUserExists(userId);
        Cart cart = findActiveCart(userId);
        cart.getItems().clear();
        return toResponse(cartRepository.save(cart));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotal(UUID userId) {
        ensureUserExists(userId);
        return calculateTotal(findActiveCart(userId));
    }

    private Cart createActiveCart(UUID userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setStatus(CartStatus.ACTIVE);
        return cartRepository.save(cart);
    }

    private void ensureUserExists(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (!userServiceClient.userExists(userId)) {
            throw new UserServiceNotFoundException("User not found for cart operation: " + userId, "USER_NOT_FOUND");
        }
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

    private int getAvailableStock(UUID productId) {
        InventoryServiceResponse inventory = inventoryServiceClient.getInventoryByProductId(productId);
        return inventory.quantity() - inventory.reservedQuantity();
    }

    private void validateAvailableStock(UUID productId, int requestedFinalQuantity, int availableStock) {
        if (requestedFinalQuantity > availableStock) {
            throw new InventoryInsufficientStockException(
                    "Insufficient stock available for product: " + productId,
                    "INSUFFICIENT_STOCK");
        }
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
