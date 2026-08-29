package com.bookecommerce.order_service.repository;

import com.bookecommerce.order_service.entity.Order;
import com.bookecommerce.order_service.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserId(UUID userId);
    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);
}
