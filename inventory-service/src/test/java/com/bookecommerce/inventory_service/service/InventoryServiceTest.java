package com.bookecommerce.inventory_service.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bookecommerce.inventory_service.dto.InventoryCreateRequest;
import com.bookecommerce.inventory_service.dto.InventoryResponse;
import com.bookecommerce.inventory_service.dto.ReservationRequest;
import com.bookecommerce.inventory_service.entity.Inventory;
import com.bookecommerce.inventory_service.entity.InventoryStatus;
import com.bookecommerce.inventory_service.entity.Reservation;
import com.bookecommerce.inventory_service.entity.ReservationStatus;
import com.bookecommerce.inventory_service.exception.ProductServiceUnavailableException;
import com.bookecommerce.inventory_service.exception.ResourceNotFoundException;
import com.bookecommerce.inventory_service.integration.product.ProductCatalogValidator;
import com.bookecommerce.inventory_service.repository.InventoryRepository;
import com.bookecommerce.inventory_service.repository.ReservationRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductCatalogValidator productCatalogValidator;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        lenient().when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(inventoryRepository.findByProductIdForUpdate(any(UUID.class))).thenReturn(Optional.empty());
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
    void createInventoryRejectsUnknownProductId() {
        InventoryCreateRequest request = new InventoryCreateRequest(productId, 5);

        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Product not found for productId: " + productId, "PRODUCT_NOT_FOUND"))
                .when(productCatalogValidator).validateProductExists(productId);

        assertThatThrownBy(() -> inventoryService.createInventory(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void createInventoryRejectsInvalidProductId() {
        InventoryCreateRequest request = new InventoryCreateRequest(null, 5);

        assertThatThrownBy(() -> inventoryService.createInventory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productId is required");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void createInventoryRejectsWhenProductServiceUnavailable() {
        InventoryCreateRequest request = new InventoryCreateRequest(productId, 5);

        org.mockito.Mockito.doThrow(new ProductServiceUnavailableException(
                "Product service unavailable for productId: " + productId,
                "PRODUCT_SERVICE_UNAVAILABLE"))
                .when(productCatalogValidator).validateProductExists(productId);

        assertThatThrownBy(() -> inventoryService.createInventory(request))
                .isInstanceOf(ProductServiceUnavailableException.class)
                .hasMessageContaining("Product service unavailable");

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
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

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
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

        InventoryResponse reserved = inventoryService.reserveStock(new ReservationRequest(productId, 4));

        assertThat(reserved.reservedQuantity()).isEqualTo(6);
        assertThat(reserved.quantity()).isEqualTo(10);
        assertThat(reserved.status()).isEqualTo(InventoryStatus.AVAILABLE);
        assertThat(reserved.reservationId()).isNotNull();
        assertThat(reserved.reservationStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void reserveInsufficientStock() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 7, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserveStock(new ReservationRequest(productId, 4)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void missingInventoryRejected() {
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserveStock(new ReservationRequest(productId, 1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Inventory not found");

        assertThatThrownBy(() -> inventoryService.releaseReservation(productId, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Inventory not found");
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
    void releaseInvalidQuantityRejected() {
        assertThatThrownBy(() -> inventoryService.releaseReservation(productId, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("greater than 0");

        assertThatThrownBy(() -> inventoryService.releaseReservation(productId, 0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than 0");

        assertThatThrownBy(() -> inventoryService.releaseReservation(productId, -1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void releaseReservationSuccessfully() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 3, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

        InventoryResponse released = inventoryService.releaseReservation(productId, 2);

        assertThat(released.reservedQuantity()).isEqualTo(1);
        assertThat(released.status()).isEqualTo(InventoryStatus.AVAILABLE);
    }

    @Test
    void releaseMoreThanReservedQuantityRejected() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 2, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.releaseReservation(productId, 3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot release");
    }

    @Test
    void confirmReservationConsumesStock() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 3, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

        InventoryResponse confirmed = inventoryService.confirmReservation(productId, 2);

        assertThat(confirmed.quantity()).isEqualTo(8);
        assertThat(confirmed.reservedQuantity()).isEqualTo(1);
        assertThat(confirmed.status()).isEqualTo(InventoryStatus.AVAILABLE);
    }

    @Test
    void confirmMoreThanReservedQuantityRejected() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 2, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.confirmReservation(productId, 3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot confirm");
    }

    @Test
    void confirmNullQuantityRejected() {
        assertThatThrownBy(() -> inventoryService.confirmReservation(productId, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void inventoryValuesRemainConsistentAfterEachOperation() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 2, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

        InventoryResponse reserved = inventoryService.reserveStock(new ReservationRequest(productId, 3));
        assertThat(reserved.quantity()).isEqualTo(10);
        assertThat(reserved.reservedQuantity()).isEqualTo(5);
        assertThat(reserved.reservedQuantity()).isLessThanOrEqualTo(reserved.quantity());

        InventoryResponse released = inventoryService.releaseReservation(productId, 2);
        assertThat(released.reservedQuantity()).isEqualTo(3);
        assertThat(released.reservedQuantity()).isGreaterThanOrEqualTo(0);

        InventoryResponse confirmed = inventoryService.confirmReservation(productId, 1);
        assertThat(confirmed.quantity()).isEqualTo(9);
        assertThat(confirmed.reservedQuantity()).isEqualTo(2);
        assertThat(confirmed.quantity()).isGreaterThanOrEqualTo(confirmed.reservedQuantity());
    }

    @Test
    void preventNegativeQuantity() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 5, 0, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

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

        @Test
        void reservationCreatesUniqueReservationIdsAndPreservesOrderCorrelation() {
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 0, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(inventoryRepository.findByProductIdForUpdate(productId))
            .thenReturn(Optional.of(inventory), Optional.of(inventory));

        InventoryResponse first = inventoryService.reserveStock(new ReservationRequest(productId, 2, UUID.randomUUID()));
        InventoryResponse second = inventoryService.reserveStock(new ReservationRequest(productId, 1, UUID.randomUUID()));

        assertThat(first.reservationId()).isNotNull();
        assertThat(second.reservationId()).isNotNull().isNotEqualTo(first.reservationId());
        assertThat(inventory.getReservedQuantity()).isEqualTo(3);
        }

        @Test
        void releaseByReservationIdChangesReservationOnce() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = new Reservation(reservationId, productId, 2, null,
            ReservationStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 2, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(reservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

        InventoryResponse response = inventoryService.releaseReservation(reservationId);

        assertThat(response.reservedQuantity()).isZero();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThatThrownBy(() -> inventoryService.releaseReservation(reservationId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("already released");
        assertThat(inventory.getReservedQuantity()).isZero();
        }

        @Test
        void confirmByReservationIdConsumesStockOnce() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = new Reservation(reservationId, productId, 2, null,
            ReservationStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        Inventory inventory = new Inventory(UUID.randomUUID(), productId, 10, 2, InventoryStatus.AVAILABLE, LocalDateTime.now());
        when(reservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

        InventoryResponse response = inventoryService.confirmReservation(reservationId);

        assertThat(response.quantity()).isEqualTo(8);
        assertThat(response.reservedQuantity()).isZero();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThatThrownBy(() -> inventoryService.confirmReservation(reservationId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("already confirmed");
        assertThat(inventory.getQuantity()).isEqualTo(8);
        }

        @Test
        void reservationAwareOperationsRejectMissingReservation() {
        UUID reservationId = UUID.randomUUID();
        when(reservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.releaseReservation(reservationId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Reservation not found");
        assertThatThrownBy(() -> inventoryService.confirmReservation(reservationId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Reservation not found");
        }

        @Test
        void reservationAwareOperationsRejectInvalidState() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = new Reservation(reservationId, productId, 2, null,
            ReservationStatus.CONFIRMED, LocalDateTime.now(), LocalDateTime.now());
        when(reservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> inventoryService.releaseReservation(reservationId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("already confirmed");
        }
}
