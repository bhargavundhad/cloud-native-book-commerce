package com.bookecommerce.cart_service.integration.product;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class ProductServiceClient {

    private final RestClient restClient;

    public ProductServiceClient(@Value("${app.service-urls.product}") String productServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(productServiceBaseUrl)
                .build();
    }

    public ProductServiceResponse getBookById(UUID productId) {
        try {
            ProductServiceResponse response = restClient.get()
                    .uri("/api/books/{productId}", productId)
                    .retrieve()
                    .body(ProductServiceResponse.class);

            if (response != null) {
                return response;
            }

            throw new ProductServiceIntegrationException("Product service returned an unexpected response for product: " + productId,
                    "PRODUCT_SERVICE_UNAVAILABLE");
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ProductServiceNotFoundException("Product not found: " + productId, "PRODUCT_NOT_FOUND");
        } catch (HttpClientErrorException.BadRequest exception) {
            throw new ProductServiceIntegrationException("Invalid product identifier: " + productId, "INVALID_PRODUCT_ID");
        } catch (HttpServerErrorException exception) {
            throw new ProductServiceIntegrationException("Product service unavailable for product: " + productId,
                    "PRODUCT_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException exception) {
            throw new ProductServiceIntegrationException("Product service unavailable for product: " + productId,
                    "PRODUCT_SERVICE_UNAVAILABLE");
        }
    }
}
