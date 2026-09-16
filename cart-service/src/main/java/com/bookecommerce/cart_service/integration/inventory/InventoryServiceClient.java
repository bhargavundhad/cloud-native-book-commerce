package com.bookecommerce.cart_service.integration.inventory;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class InventoryServiceClient {

    private final RestClient restClient;

    public InventoryServiceClient(@Value("${app.service-urls.inventory}") String inventoryServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(inventoryServiceBaseUrl)
                .build();
    }

    public InventoryServiceResponse getInventoryByProductId(UUID productId) {
        try {
            InventoryServiceResponse response = restClient.get()
                    .uri("/api/inventory/product/{productId}", productId)
                    .retrieve()
                    .body(InventoryServiceResponse.class);

            if (response != null) {
                return response;
            }

            throw new InventoryServiceIntegrationException(
                    "Inventory service returned an unexpected response for product: " + productId,
                    "INVENTORY_SERVICE_UNAVAILABLE");
        } catch (HttpClientErrorException.NotFound exception) {
            throw new InventoryServiceNotFoundException("Inventory not found for product: " + productId, "INVENTORY_NOT_FOUND");
        } catch (HttpClientErrorException.BadRequest exception) {
            throw new InventoryServiceIntegrationException("Invalid inventory identifier: " + productId, "INVALID_PRODUCT_ID");
        } catch (HttpServerErrorException exception) {
            throw new InventoryServiceIntegrationException("Inventory service unavailable for product: " + productId,
                    "INVENTORY_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException exception) {
            throw new InventoryServiceIntegrationException("Inventory service unavailable for product: " + productId,
                    "INVENTORY_SERVICE_UNAVAILABLE");
        }
    }
}
