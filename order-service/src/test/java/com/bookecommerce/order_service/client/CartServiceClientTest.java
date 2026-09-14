package com.bookecommerce.order_service.client;

import com.bookecommerce.order_service.client.dto.CartResponseDto;
import com.bookecommerce.order_service.exception.CartNotFoundException;
import com.bookecommerce.order_service.exception.CartServiceUnavailableException;
import com.bookecommerce.order_service.exception.UserUnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CartServiceClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private CartServiceClient cartServiceClient;
    private static final String BASE_URL = "http://localhost:8084";

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        cartServiceClient = new CartServiceClient(restClientBuilder.build(), BASE_URL);
    }

    @Test
    @DisplayName("Should forward Authorization header and return CartResponseDto when Cart Service responds with 200 OK")
    void testGetActiveCartSuccessWithBearerToken() {
        UUID userId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID cartItemId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        String jsonResponse = """
                {
                    "id": "%s",
                    "userId": "%s",
                    "status": "ACTIVE",
                    "items": [
                        {
                            "id": "%s",
                            "productId": "%s",
                            "quantity": 2,
                            "unitPrice": 499.00
                        }
                    ],
                    "total": 998.00
                }
                """.formatted(cartId, userId, cartItemId, productId);

        mockServer.expect(requestTo(BASE_URL + "/api/carts/" + userId + "/active"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        CartResponseDto cart = cartServiceClient.getActiveCart(userId, bearerToken);

        assertNotNull(cart);
        assertEquals(cartId, cart.id());
        assertEquals(userId, cart.userId());
        assertEquals("ACTIVE", cart.status());
        assertEquals(1, cart.items().size());
        assertEquals(productId, cart.items().get(0).productId());
        assertEquals(2, cart.items().get(0).quantity());
        assertEquals(new BigDecimal("499.00"), cart.items().get(0).unitPrice());
        assertEquals(new BigDecimal("998.00"), cart.total());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw CartNotFoundException when Cart Service returns 404 NOT_FOUND")
    void testGetActiveCartNotFound() {
        UUID userId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        mockServer.expect(requestTo(BASE_URL + "/api/carts/" + userId + "/active"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(CartNotFoundException.class, () -> cartServiceClient.getActiveCart(userId, bearerToken));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw UserUnauthorizedException when Cart Service returns 401 UNAUTHORIZED")
    void testGetActiveCartUnauthorized() {
        UUID userId = UUID.randomUUID();

        mockServer.expect(requestTo(BASE_URL + "/api/carts/" + userId + "/active"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(UserUnauthorizedException.class, () -> cartServiceClient.getActiveCart(userId, null));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw CartServiceUnavailableException when Cart Service returns 500 Internal Server Error")
    void testGetActiveCartServerError() {
        UUID userId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        mockServer.expect(requestTo(BASE_URL + "/api/carts/" + userId + "/active"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withServerError());

        assertThrows(CartServiceUnavailableException.class, () -> cartServiceClient.getActiveCart(userId, bearerToken));
        mockServer.verify();
    }
}
