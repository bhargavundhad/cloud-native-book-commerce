package com.bookecommerce.inventory_service.integration.product;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.bookecommerce.inventory_service.exception.InvalidInventoryStateException;
import com.bookecommerce.inventory_service.exception.ProductServiceUnavailableException;
import com.bookecommerce.inventory_service.exception.ResourceNotFoundException;

@Component
public class ProductCatalogValidator {

    private final RestClient restClient;

    ProductCatalogValidator(RestClient restClient) {
        this.restClient = restClient;
    }

    @Autowired
    public ProductCatalogValidator(@Value("${app.service-urls.product}") String productServiceBaseUrl) {
        this(RestClient.builder()
                .baseUrl(productServiceBaseUrl)
                .build());
    }

    public void validateProductExists(UUID productId) {
        try {
            restClient.get()
                    .uri("/api/books/{productId}", productId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException("Product not found for productId: " + productId, "PRODUCT_NOT_FOUND");
        } catch (HttpClientErrorException.BadRequest exception) {
            throw new InvalidInventoryStateException("Invalid product identifier: " + productId, "INVALID_PRODUCT_ID");
        } catch (HttpServerErrorException exception) {
            throw new ProductServiceUnavailableException(
                    "Product service unavailable for productId: " + productId,
                    "PRODUCT_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException exception) {
            throw new ProductServiceUnavailableException(
                    "Product service unavailable for productId: " + productId,
                    "PRODUCT_SERVICE_UNAVAILABLE");
        }
    }
}
