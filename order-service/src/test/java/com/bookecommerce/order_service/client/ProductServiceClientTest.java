package com.bookecommerce.order_service.client;

import com.bookecommerce.order_service.client.dto.ProductResponseDto;
import com.bookecommerce.order_service.exception.ProductNotFoundException;
import com.bookecommerce.order_service.exception.ProductServiceUnavailableException;
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

class ProductServiceClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private ProductServiceClient productServiceClient;
    private static final String BASE_URL = "http://localhost:8082";

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        productServiceClient = new ProductServiceClient(restClientBuilder.build(), BASE_URL);
    }

    @Test
    @DisplayName("Should forward Authorization header and return ProductResponseDto when Product Service responds with 200 OK")
    void testGetProductSuccessWithBearerToken() {
        UUID productId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        String jsonResponse = """
                {
                    "id": "%s",
                    "isbn": "9781234567890",
                    "title": "Clean Code",
                    "description": "Software engineering principles",
                    "authorId": "%s",
                    "categoryId": "%s",
                    "price": 599.00
                }
                """.formatted(productId, authorId, categoryId);

        mockServer.expect(requestTo(BASE_URL + "/api/books/" + productId))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ProductResponseDto product = productServiceClient.getProduct(productId, bearerToken);

        assertNotNull(product);
        assertEquals(productId, product.id());
        assertEquals("9781234567890", product.isbn());
        assertEquals("Clean Code", product.title());
        assertEquals(authorId, product.authorId());
        assertEquals(categoryId, product.categoryId());
        assertEquals(new BigDecimal("599.00"), product.price());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when Product Service returns 404 NOT_FOUND")
    void testGetProductNotFound() {
        UUID productId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        mockServer.expect(requestTo(BASE_URL + "/api/books/" + productId))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(ProductNotFoundException.class, () -> productServiceClient.getProduct(productId, bearerToken));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw UserUnauthorizedException when Product Service returns 401 UNAUTHORIZED")
    void testGetProductUnauthorized() {
        UUID productId = UUID.randomUUID();

        mockServer.expect(requestTo(BASE_URL + "/api/books/" + productId))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(UserUnauthorizedException.class, () -> productServiceClient.getProduct(productId, null));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw ProductServiceUnavailableException when Product Service returns 500 Internal Server Error")
    void testGetProductServerError() {
        UUID productId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        mockServer.expect(requestTo(BASE_URL + "/api/books/" + productId))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withServerError());

        assertThrows(ProductServiceUnavailableException.class, () -> productServiceClient.getProduct(productId, bearerToken));
        mockServer.verify();
    }
}
