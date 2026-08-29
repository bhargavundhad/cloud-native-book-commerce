package com.bookecommerce.inventory_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;

import com.bookecommerce.inventory_service.dto.InventoryCreateRequest;
import com.bookecommerce.inventory_service.dto.InventoryResponse;
import com.bookecommerce.inventory_service.dto.ReservationRequest;
import com.bookecommerce.inventory_service.entity.Inventory;
import com.bookecommerce.inventory_service.entity.InventoryStatus;
import com.bookecommerce.inventory_service.repository.InventoryRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        lenient().when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createInventorySuccessfully() {
        InventoryCreateRequest request = new InventoryCreateRequest(productId, 10);
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 0, InventoryStatus.AVAILABLE, LocalDateTime.now());

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        InventoryResponse response = inventoryService.createInventory(request);

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.quantity()).isEqualTo(10);
        assertThat(response.reservedQuantity()).isZero();
        assertThat(response.status()).isEqualTo(InventoryStatus.AVAILABLE);
    }

    @Test
    void duplicateProductInventoryRejected() {
        InventoryCreateRequest request = new InventoryCreateRequest(productId, 5);
        Inventory existing = new Inventory(UUID.randomUUID(), productId, 8, 0, InventoryStatus.AVAILABLE, LocalDateTime.now());

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> inventoryService.createInventory(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void getInventory() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 4, 1, InventoryStatus.AVAILABLE, LocalDateTime.now());

        when(inventoryRepository.findById(inventory.getId())).thenReturn(Optional.of(inventory));
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        assertThat(inventoryService.getInventoryById(inventory.getId()).productId()).isEqualTo(productId);
        assertThat(inventoryService.getInventoryByProductId(productId).productId()).isEqualTo(productId);
    }

    @Test
    void addAndUpdateStock() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 5, 1, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        InventoryResponse added = inventoryService.addStock(productId, 3);
        assertThat(added.quantity()).isEqualTo(8);

        InventoryResponse updated = inventoryService.updateStock(productId, 12);
        assertThat(updated.quantity()).isEqualTo(12);
        assertThat(updated.reservedQuantity()).isEqualTo(1);
        assertThat(updated.status()).isEqualTo(InventoryStatus.AVAILABLE);
    }

    @Test
    void reserveSufficientStock() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 2, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        InventoryResponse reserved = inventoryService.reserveStock(new ReservationRequest(productId, 4));

        assertThat(reserved.reservedQuantity()).isEqualTo(6);
        assertThat(reserved.quantity()).isEqualTo(10);
        assertThat(reserved.status()).isEqualTo(InventoryStatus.AVAILABLE);
    }

    @Test
    void reserveInsufficientStock() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 7, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserveStock(new ReservationRequest(productId, 4)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void reserveZeroOrNegativeQuantityRejected() {
        assertThatThrownBy(() -> inventoryService.reserveStock(new ReservationRequest(productId, 0)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than 0");

        assertThatThrownBy(() -> inventoryService.reserveStock(new ReservationRequest(productId, -1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void releaseReservationSuccessfully() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 3, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        InventoryResponse released = inventoryService.releaseReservation(productId, 2);

        assertThat(released.reservedQuantity()).isEqualTo(1);
        assertThat(released.status()).isEqualTo(InventoryStatus.AVAILABLE);
    }

    @Test
    void releaseMoreThanReservedQuantityRejected() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 2, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.releaseReservation(productId, 3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot release");
    }

    @Test
    void confirmReservationConsumesStock() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 3, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        InventoryResponse confirmed = inventoryService.confirmReservation(productId, 2);

        assertThat(confirmed.quantity()).isEqualTo(8);
        assertThat(confirmed.reservedQuantity()).isEqualTo(1);
        assertThat(confirmed.status()).isEqualTo(InventoryStatus.AVAILABLE);
    }

    @Test
    void preventNegativeQuantity() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 5, 0, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.updateStock(productId, -1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("negative");

        assertThatThrownBy(() -> inventoryService.confirmReservation(productId, 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot confirm");
    }

    @Test
    void statusReflectsAvailableStock() {
        Inventory available = new Inventory(UUID.randomUUID(), productId, 3, 0, InventoryStatus.AVAILABLE, LocalDateTime.now());
        Inventory outOfStock = new Inventory(UUID.randomUUID(), UUID.randomUUID(), 0, 0, InventoryStatus.OUT_OF_STOCK, LocalDateTime.now());

        assertThat(inventoryService.resolveStatus(available)).isEqualTo(InventoryStatus.AVAILABLE);
        assertThat(inventoryService.resolveStatus(outOfStock)).isEqualTo(InventoryStatus.OUT_OF_STOCK);
    }
}
