package com.bookecommerce.inventory_service.integration.product;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.bookecommerce.inventory_service.exception.InvalidInventoryStateException;
import com.bookecommerce.inventory_service.exception.ProductServiceUnavailableException;
import com.bookecommerce.inventory_service.exception.ResourceNotFoundException;

class ProductCatalogValidatorTest {

    @Test
    void validateProductExistsAllowsExistingProduct() {
        UUID productId = UUID.randomUUID();
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/books/{productId}", productId)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        ProductCatalogValidator validator = new ProductCatalogValidator(restClient);

        assertThatCode(() -> validator.validateProductExists(productId)).doesNotThrowAnyException();
    }

    @Test
    void validateProductExistsRejectsMissingProduct() {
        UUID productId = UUID.randomUUID();
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/books/{productId}", productId)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        ProductCatalogValidator validator = new ProductCatalogValidator(restClient);

        assertThatThrownBy(() -> validator.validateProductExists(productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void validateProductExistsRejectsInvalidProductId() {
        UUID productId = UUID.randomUUID();
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/books/{productId}", productId)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad request", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        ProductCatalogValidator validator = new ProductCatalogValidator(restClient);

        assertThatThrownBy(() -> validator.validateProductExists(productId))
                .isInstanceOf(InvalidInventoryStateException.class)
                .hasMessageContaining("Invalid product identifier");
    }

    @Test
    void validateProductExistsRejectsUnavailableProductService() {
        UUID productId = UUID.randomUUID();
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/books/{productId}", productId)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenThrow(new ResourceAccessException("service unavailable"));

        ProductCatalogValidator validator = new ProductCatalogValidator(restClient);

        assertThatThrownBy(() -> validator.validateProductExists(productId))
                .isInstanceOf(ProductServiceUnavailableException.class)
                .hasMessageContaining("Product service unavailable");
    }
}
