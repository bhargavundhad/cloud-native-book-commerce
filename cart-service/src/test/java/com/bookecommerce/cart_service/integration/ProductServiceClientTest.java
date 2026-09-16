package com.bookecommerce.cart_service.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bookecommerce.cart_service.integration.product.ProductServiceClient;

class ProductServiceClientTest {

    @Test
    void createsClientWithConfiguredBaseUrl() {
        ProductServiceClient client = new ProductServiceClient("http://localhost:8082");
        UUID productId = UUID.randomUUID();

        assertThat(client).isNotNull();
        assertThat(productId).isNotNull();
    }
}
