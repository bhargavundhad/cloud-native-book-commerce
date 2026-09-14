package com.bookecommerce.inventory_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bookecommerce.inventory_service.entity.Inventory;

import jakarta.persistence.LockModeType;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

	Optional<Inventory> findByProductId(UUID productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Inventory i where i.productId = :productId")
	Optional<Inventory> findByProductIdForUpdate(@Param("productId") UUID productId);
}