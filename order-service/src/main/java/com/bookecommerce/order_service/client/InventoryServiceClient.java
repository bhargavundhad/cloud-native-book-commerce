package com.bookecommerce.order_service.client;

import com.bookecommerce.order_service.client.dto.InventoryResponseDto;
import com.bookecommerce.order_service.client.dto.ReservationRequestDto;
import com.bookecommerce.order_service.exception.InsufficientStockException;
import com.bookecommerce.order_service.exception.InventoryNotFoundException;
import com.bookecommerce.order_service.exception.InventoryServiceUnavailableException;
import com.bookecommerce.order_service.exception.UserUnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
public class InventoryServiceClient {

    private final RestClient restClient;
    private final String inventoryServiceUrl;

    public InventoryServiceClient(
            RestClient restClient,
            @Value("${services.inventory-service.url:http://localhost:8083}") String inventoryServiceUrl) {
        this.restClient = restClient;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    public InventoryResponseDto reserveStock(UUID productId, Integer quantity) {
        return reserveStock(productId, quantity, null);
    }

    public InventoryResponseDto reserveStock(UUID productId, Integer quantity, String explicitAuthHeader) {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }

        String authHeader = (explicitAuthHeader != null && !explicitAuthHeader.isBlank())
                ? explicitAuthHeader
                : getIncomingAuthorizationHeader();

        ReservationRequestDto requestDto = new ReservationRequestDto(productId, quantity, null);

        try {
            RestClient.RequestBodySpec requestSpec = restClient.post()
                    .uri(inventoryServiceUrl + "/api/inventory/reserve")
                    .body(requestDto);

            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            InventoryResponseDto response = requestSpec
                    .retrieve()
                    .onStatus(status -> status.equals(HttpStatus.BAD_REQUEST), (req, resp) -> {
                        throw new InsufficientStockException("Insufficient stock for productId: " + productId, productId);
                    })
                    .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), (req, resp) -> {
                        throw new InventoryNotFoundException(productId);
                    })
                    .onStatus(status -> status.equals(HttpStatus.UNAUTHORIZED) || status.equals(HttpStatus.FORBIDDEN), (req, resp) -> {
                        throw new UserUnauthorizedException("Authentication required by Inventory Service");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new InventoryServiceUnavailableException("Inventory Service returned server error");
                    })
                    .body(InventoryResponseDto.class);

            if (response == null) {
                throw new InventoryNotFoundException(productId);
            }

            return response;
        } catch (InsufficientStockException | InventoryNotFoundException | UserUnauthorizedException | InventoryServiceUnavailableException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new InsufficientStockException("Insufficient stock for productId: " + productId, productId);
            } else if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InventoryNotFoundException(productId);
            } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new UserUnauthorizedException("Authentication required by Inventory Service");
            }
            throw new InventoryServiceUnavailableException("Inventory Service error: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            throw new InventoryServiceUnavailableException("Inventory Service is unreachable at " + inventoryServiceUrl, ex);
        } catch (Exception ex) {
            throw new InventoryServiceUnavailableException("Failed to communicate with Inventory Service: " + ex.getMessage(), ex);
        }
    }

    public InventoryResponseDto releaseReservation(UUID reservationId) {
        return releaseReservation(reservationId, null);
    }

    public InventoryResponseDto releaseReservation(UUID reservationId, String explicitAuthHeader) {
        if (reservationId == null) {
            throw new IllegalArgumentException("reservationId is required");
        }

        String authHeader = (explicitAuthHeader != null && !explicitAuthHeader.isBlank())
                ? explicitAuthHeader
                : getIncomingAuthorizationHeader();

        try {
            RestClient.RequestBodySpec requestSpec = restClient.post()
                    .uri(inventoryServiceUrl + "/api/inventory/release/{reservationId}", reservationId);

            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            return requestSpec
                    .retrieve()
                    .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), (req, resp) -> {
                        throw new InventoryNotFoundException("Reservation not found for reservationId: " + reservationId, null);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new InventoryServiceUnavailableException("Inventory Service returned server error on release");
                    })
                    .body(InventoryResponseDto.class);
        } catch (InventoryNotFoundException | InventoryServiceUnavailableException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InventoryNotFoundException("Reservation not found for reservationId: " + reservationId, null);
            }
            throw new InventoryServiceUnavailableException("Inventory Service release error: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            throw new InventoryServiceUnavailableException("Inventory Service is unreachable at " + inventoryServiceUrl, ex);
        } catch (Exception ex) {
            throw new InventoryServiceUnavailableException("Failed to release reservation: " + ex.getMessage(), ex);
        }
    }

    private String getIncomingAuthorizationHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest() != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader(HttpHeaders.AUTHORIZATION);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
