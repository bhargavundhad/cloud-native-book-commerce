package com.bookecommerce.inventory_service.service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookecommerce.inventory_service.dto.InventoryCreateRequest;
import com.bookecommerce.inventory_service.dto.InventoryResponse;
import com.bookecommerce.inventory_service.dto.ReservationRequest;
import com.bookecommerce.inventory_service.entity.Inventory;
import com.bookecommerce.inventory_service.entity.InventoryStatus;
import com.bookecommerce.inventory_service.exception.ConflictException;
import com.bookecommerce.inventory_service.exception.InvalidInventoryStateException;
import com.bookecommerce.inventory_service.exception.ResourceNotFoundException;
import com.bookecommerce.inventory_service.repository.InventoryRepository;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public InventoryResponse createInventory(InventoryCreateRequest request) {
        validateProductId(request.productId());
        validateQuantity(request.quantity(), "Create inventory quantity");

        if (inventoryRepository.findByProductId(request.productId()).isPresent()) {
            throw new ConflictException("Inventory already exists for productId: " + request.productId(), "INVENTORY_DUPLICATE");
        }

        Inventory inventory = new Inventory(
                UUID.randomUUID(),
                request.productId(),
                request.quantity(),
                0,
                resolveStatusFromNumbers(request.quantity(), 0),
                LocalDateTime.now());

        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(UUID id) {
        validateId(id, "inventoryId");
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for id: " + id, "INVENTORY_NOT_FOUND"));
        return toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(UUID productId) {
        validateProductId(productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId: " + productId, "INVENTORY_NOT_FOUND"));
        return toResponse(inventory);
    }

    @Transactional
    public InventoryResponse addStock(UUID productId, Integer quantityToAdd) {
        validateProductId(productId);
        validateQuantity(quantityToAdd, "Stock to add");

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId: " + productId, "INVENTORY_NOT_FOUND"));

        int updatedQuantity = inventory.getQuantity() + quantityToAdd;
        if (updatedQuantity < 0) {
            throw new InvalidInventoryStateException("Quantity cannot be negative", "INVALID_QUANTITY");
        }

        inventory.setQuantity(updatedQuantity);
        inventory.setStatus(resolveStatus(inventory));
        inventory.setUpdatedAt(LocalDateTime.now());
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse updateStock(UUID productId, Integer targetQuantity) {
        validateProductId(productId);
        validateSetQuantity(targetQuantity);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId: " + productId, "INVENTORY_NOT_FOUND"));

        if (targetQuantity < 0) {
            throw new InvalidInventoryStateException("Quantity cannot be negative", "INVALID_QUANTITY");
        }
        if (targetQuantity < inventory.getReservedQuantity()) {
            throw new InvalidInventoryStateException("Quantity cannot be less than reservedQuantity", "INVALID_STATE");
        }

        inventory.setQuantity(targetQuantity);
        inventory.setStatus(resolveStatus(inventory));
        inventory.setUpdatedAt(LocalDateTime.now());
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse reserveStock(ReservationRequest request) {
        validateProductId(request.productId());
        if (request.quantity() <= 0) {
            throw new InvalidInventoryStateException("Reservation quantity must be greater than 0", "INVALID_RESERVATION");
        }

        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId: " + request.productId(), "INVENTORY_NOT_FOUND"));

        int availableStock = inventory.getQuantity() - inventory.getReservedQuantity();
        if (request.quantity() > availableStock) {
            throw new InvalidInventoryStateException("Insufficient stock for reservation. Requested: " + request.quantity() + ", available: " + availableStock, "INSUFFICIENT_STOCK");
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        inventory.setStatus(resolveStatus(inventory));
        inventory.setUpdatedAt(LocalDateTime.now());
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse releaseReservation(UUID productId, Integer quantityToRelease) {
        validateProductId(productId);
        if (quantityToRelease <= 0) {
            throw new InvalidInventoryStateException("Release quantity must be greater than 0", "INVALID_RELEASE");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId: " + productId, "INVENTORY_NOT_FOUND"));

        if (quantityToRelease > inventory.getReservedQuantity()) {
            throw new InvalidInventoryStateException("Cannot release more than the currently reserved quantity", "INVALID_RELEASE");
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantityToRelease);
        inventory.setStatus(resolveStatus(inventory));
        inventory.setUpdatedAt(LocalDateTime.now());
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse confirmReservation(UUID productId, Integer confirmedQuantity) {
        validateProductId(productId);
        if (confirmedQuantity <= 0) {
            throw new InvalidInventoryStateException("Confirmation quantity must be greater than 0", "INVALID_CONFIRMATION");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId: " + productId, "INVENTORY_NOT_FOUND"));

        if (confirmedQuantity > inventory.getReservedQuantity()) {
            throw new InvalidInventoryStateException("Cannot confirm more than the currently reserved quantity", "INVALID_CONFIRMATION");
        }

        int updatedQuantity = inventory.getQuantity() - confirmedQuantity;
        int updatedReserved = inventory.getReservedQuantity() - confirmedQuantity;
        if (updatedQuantity < 0 || updatedReserved < 0) {
            throw new InvalidInventoryStateException("Confirmed quantity would result in negative values", "INVALID_CONFIRMATION");
        }

        inventory.setQuantity(updatedQuantity);
        inventory.setReservedQuantity(updatedReserved);
        inventory.setStatus(resolveStatus(inventory));
        inventory.setUpdatedAt(LocalDateTime.now());
        return toResponse(inventoryRepository.save(inventory));
    }

    public InventoryStatus resolveStatus(Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory must not be null");
        return resolveStatusFromNumbers(inventory.getQuantity(), inventory.getReservedQuantity());
    }

    InventoryStatus resolveStatusFromNumbers(Integer quantity, Integer reservedQuantity) {
        int available = quantity - reservedQuantity;
        return available <= 0 ? InventoryStatus.OUT_OF_STOCK : InventoryStatus.AVAILABLE;
    }

    private void validateProductId(UUID productId) {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
    }

    private void validateId(UUID id, String fieldName) {
        if (id == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private void validateQuantity(Integer quantity, String label) {
        if (quantity == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        if (quantity < 0) {
            throw new InvalidInventoryStateException(label + " cannot be negative", "INVALID_QUANTITY");
        }
    }

    private void validateSetQuantity(Integer quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("quantity is required");
        }
        if (quantity < 0) {
            throw new InvalidInventoryStateException("quantity cannot be negative", "INVALID_QUANTITY");
        }
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getStatus(),
                inventory.getUpdatedAt());
    }
}
