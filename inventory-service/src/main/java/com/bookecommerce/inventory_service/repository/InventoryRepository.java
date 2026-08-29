package com.bookecommerce.inventory_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookecommerce.inventory_service.entity.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

	Optional<Inventory> findByProductId(UUID productId);
}