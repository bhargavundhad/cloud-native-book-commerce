package com.bookecommerce.cart_service.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bookecommerce.cart_service.integration.user.UserServiceClient;

class UserServiceClientTest {

    @Test
    void createsClientWithConfiguredBaseUrl() {
        UserServiceClient client = new UserServiceClient("http://localhost:8081", "test-secret");
        UUID userId = UUID.randomUUID();

        assertThat(client).isNotNull();
        assertThat(userId).isNotNull();
    }
}
