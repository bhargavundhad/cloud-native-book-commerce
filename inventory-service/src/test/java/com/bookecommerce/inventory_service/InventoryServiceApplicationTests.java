package com.bookecommerce.inventory_service;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.bookecommerce.inventory_service.entity.Inventory;
import com.bookecommerce.inventory_service.entity.InventoryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

class InventoryServiceApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void inventoryUsesDomainTypesAndStatusValues() {
		Inventory inventory = new Inventory(UUID.randomUUID(), UUID.randomUUID(), 10, 2,
				InventoryStatus.AVAILABLE, LocalDateTime.now());

		assertThat(inventory.getQuantity()).isEqualTo(10);
		assertThat(inventory.getReservedQuantity()).isEqualTo(2);
		assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.AVAILABLE);
	}

	@Test
	void inventoryMapsToApprovedTable() throws NoSuchFieldException {
		Table table = Inventory.class.getAnnotation(Table.class);
		Column productId = Inventory.class.getDeclaredField("productId").getAnnotation(Column.class);
		Column status = Inventory.class.getDeclaredField("status").getAnnotation(Column.class);
		Enumerated statusMapping = Inventory.class.getDeclaredField("status").getAnnotation(Enumerated.class);

		assertThat(Inventory.class.isAnnotationPresent(Entity.class)).isTrue();
		assertThat(table.name()).isEqualTo("inventory");
		assertThat(table.schema()).isEqualTo("inventory_schema");
		assertThat(productId.name()).isEqualTo("product_id");
		assertThat(productId.unique()).isTrue();
		assertThat(status.name()).isEqualTo("status");
		assertThat(status.length()).isEqualTo(20);
		assertThat(statusMapping.value()).isEqualTo(EnumType.STRING);
	}

}
