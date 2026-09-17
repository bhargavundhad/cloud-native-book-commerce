package com.bookecommerce.order_service.client;

import com.bookecommerce.order_service.client.dto.InventoryResponseDto;
import com.bookecommerce.order_service.exception.InsufficientStockException;
import com.bookecommerce.order_service.exception.InventoryNotFoundException;
import com.bookecommerce.order_service.exception.InventoryServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class InventoryServiceClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private InventoryServiceClient inventoryServiceClient;
    private static final String BASE_URL = "http://localhost:8083";

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        inventoryServiceClient = new InventoryServiceClient(restClientBuilder.build(), BASE_URL);
    }

    @Test
    @DisplayName("Should forward Authorization header and return InventoryResponseDto when Inventory Service reserves stock successfully")
    void testReserveStockSuccessWithBearerToken() {
        UUID productId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        String jsonResponse = """
                {
                    "id": "%s",
                    "productId": "%s",
                    "quantity": 100,
                    "reservedQuantity": 2,
                    "status": "AVAILABLE",
                    "updatedAt": "2026-09-14T22:00:00",
                    "reservationId": "%s",
                    "orderId": null,
                    "reservationQuantity": 2,
                    "reservationStatus": "ACTIVE",
                    "reservationCreatedAt": "2026-09-14T22:00:00",
                    "reservationUpdatedAt": "2026-09-14T22:00:00"
                }
                """.formatted(inventoryId, productId, reservationId);

        mockServer.expect(requestTo(BASE_URL + "/api/inventory/reserve"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.orderId").isEmpty())
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        InventoryResponseDto response = inventoryServiceClient.reserveStock(productId, 2, bearerToken);

        assertNotNull(response);
        assertEquals(inventoryId, response.id());
        assertEquals(productId, response.productId());
        assertEquals(reservationId, response.reservationId());
        assertEquals("ACTIVE", response.reservationStatus());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when Inventory Service returns 400 BAD_REQUEST")
    void testReserveStockInsufficientStock() {
        UUID productId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        mockServer.expect(requestTo(BASE_URL + "/api/inventory/reserve"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(InsufficientStockException.class, () -> inventoryServiceClient.reserveStock(productId, 5, bearerToken));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw InventoryNotFoundException when Inventory Service returns 404 NOT_FOUND")
    void testReserveStockNotFound() {
        UUID productId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        mockServer.expect(requestTo(BASE_URL + "/api/inventory/reserve"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(InventoryNotFoundException.class, () -> inventoryServiceClient.reserveStock(productId, 1, bearerToken));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw InventoryServiceUnavailableException when Inventory Service returns 500 Internal Server Error")
    void testReserveStockServerError() {
        UUID productId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        mockServer.expect(requestTo(BASE_URL + "/api/inventory/reserve"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withServerError());

        assertThrows(InventoryServiceUnavailableException.class, () -> inventoryServiceClient.reserveStock(productId, 1, bearerToken));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should release reservation successfully by reservation ID")
    void testReleaseReservationSuccess() {
        UUID reservationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        String jsonResponse = """
                {
                    "id": "%s",
                    "productId": "%s",
                    "quantity": 100,
                    "reservedQuantity": 0,
                    "status": "AVAILABLE",
                    "updatedAt": "2026-09-14T22:00:00",
                    "reservationId": "%s",
                    "orderId": null,
                    "reservationQuantity": 2,
                    "reservationStatus": "RELEASED",
                    "reservationCreatedAt": "2026-09-14T22:00:00",
                    "reservationUpdatedAt": "2026-09-14T22:00:00"
                }
                """.formatted(inventoryId, productId, reservationId);

        mockServer.expect(requestTo(BASE_URL + "/api/inventory/release/" + reservationId))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        InventoryResponseDto response = inventoryServiceClient.releaseReservation(reservationId, bearerToken);

        assertNotNull(response);
        assertEquals(reservationId, response.reservationId());
        assertEquals("RELEASED", response.reservationStatus());
        mockServer.verify();
    }
}
