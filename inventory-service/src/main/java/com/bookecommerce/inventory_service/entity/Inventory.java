package com.bookecommerce.inventory_service.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory", schema = "inventory_schema")
public class Inventory {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "product_id", nullable = false, unique = true)
	private UUID productId;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "reserved_quantity", nullable = false)
	private Integer reservedQuantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private InventoryStatus status;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Inventory() {
	}

	public Inventory(UUID id, UUID productId, Integer quantity, Integer reservedQuantity,
			InventoryStatus status, LocalDateTime updatedAt) {
		this.id = id;
		this.productId = productId;
		this.quantity = quantity;
		this.reservedQuantity = reservedQuantity;
		this.status = status;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getProductId() {
		return productId;
	}

	public void setProductId(UUID productId) {
		this.productId = productId;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Integer getReservedQuantity() {
		return reservedQuantity;
	}

	public void setReservedQuantity(Integer reservedQuantity) {
		this.reservedQuantity = reservedQuantity;
	}

	public InventoryStatus getStatus() {
		return status;
	}

	public void setStatus(InventoryStatus status) {
		this.status = status;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}