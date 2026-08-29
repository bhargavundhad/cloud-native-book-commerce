package com.bookecommerce.cart_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookecommerce.cart_service.entity.Cart;
import com.bookecommerce.cart_service.entity.CartStatus;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    List<Cart> findByUserId(UUID userId);

    Optional<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);

    boolean existsByUserIdAndStatus(UUID userId, CartStatus status);
}
