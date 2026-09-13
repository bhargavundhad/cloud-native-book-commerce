package com.bookecommerce.inventory_service.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookecommerce.inventory_service.dto.InventoryCreateRequest;
import com.bookecommerce.inventory_service.dto.InventoryResponse;
import com.bookecommerce.inventory_service.dto.ReservationRequest;
import com.bookecommerce.inventory_service.dto.StockUpdateRequest;
import com.bookecommerce.inventory_service.service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody InventoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createInventory(request));
    }

    @GetMapping("/{inventoryId}")
    public InventoryResponse getInventoryById(@PathVariable UUID inventoryId) {
        return inventoryService.getInventoryById(inventoryId);
    }

    @GetMapping("/product/{productId}")
    public InventoryResponse getInventoryByProductId(@PathVariable UUID productId) {
        return inventoryService.getInventoryByProductId(productId);
    }

    @PutMapping("/stock/{productId}")
    public InventoryResponse updateStock(@PathVariable UUID productId, @Valid @RequestBody StockUpdateRequest request) {
        return inventoryService.updateStock(productId, request.quantity());
    }

    @PostMapping("/stock/add/{productId}")
    public InventoryResponse addStock(@PathVariable UUID productId, @RequestParam Integer quantity) {
        return inventoryService.addStock(productId, quantity);
    }

    @PostMapping("/reserve")
    public InventoryResponse reserveStock(@Valid @RequestBody ReservationRequest request) {
        return inventoryService.reserveStock(request);
    }

    @PostMapping("/release")
    public InventoryResponse releaseReservation(@RequestParam UUID productId, @RequestParam Integer quantity) {
        return inventoryService.releaseReservation(productId, quantity);
    }

    @PostMapping("/release/{reservationId}")
    public InventoryResponse releaseReservationById(@PathVariable UUID reservationId) {
        return inventoryService.releaseReservation(reservationId);
    }

    @PostMapping("/confirm")
    public InventoryResponse confirmReservation(@RequestParam UUID productId, @RequestParam Integer quantity) {
        return inventoryService.confirmReservation(productId, quantity);
    }

    @PostMapping("/confirm/{reservationId}")
    public InventoryResponse confirmReservationById(@PathVariable UUID reservationId) {
        return inventoryService.confirmReservation(reservationId);
    }
}
