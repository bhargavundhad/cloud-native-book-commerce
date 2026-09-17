package com.bookecommerce.order_service.client;

import com.bookecommerce.order_service.client.dto.ProductResponseDto;
import com.bookecommerce.order_service.exception.ProductNotFoundException;
import com.bookecommerce.order_service.exception.ProductServiceUnavailableException;
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
public class ProductServiceClient {

    private final RestClient restClient;
    private final String productServiceUrl;

    public ProductServiceClient(
            RestClient restClient,
            @Value("${services.product-service.url:http://localhost:8082}") String productServiceUrl) {
        this.restClient = restClient;
        this.productServiceUrl = productServiceUrl;
    }

    public ProductResponseDto getProduct(UUID productId) {
        return getProduct(productId, null);
    }

    public ProductResponseDto getProduct(UUID productId, String explicitAuthHeader) {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }

        String authHeader = (explicitAuthHeader != null && !explicitAuthHeader.isBlank())
                ? explicitAuthHeader
                : getIncomingAuthorizationHeader();

        try {
            RestClient.RequestHeadersSpec<?> requestSpec = restClient.get()
                    .uri(productServiceUrl + "/api/books/{id}", productId);

            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            ProductResponseDto response = requestSpec
                    .retrieve()
                    .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), (req, resp) -> {
                        throw new ProductNotFoundException(productId);
                    })
                    .onStatus(status -> status.equals(HttpStatus.UNAUTHORIZED) || status.equals(HttpStatus.FORBIDDEN), (req, resp) -> {
                        throw new UserUnauthorizedException("Authentication required by Product Service");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new ProductServiceUnavailableException("Product Service returned server error");
                    })
                    .body(ProductResponseDto.class);

            if (response == null) {
                throw new ProductNotFoundException(productId);
            }

            return response;
        } catch (ProductNotFoundException | UserUnauthorizedException | ProductServiceUnavailableException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ProductNotFoundException(productId);
            } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new UserUnauthorizedException("Authentication required by Product Service");
            }
            throw new ProductServiceUnavailableException("Product Service error: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            throw new ProductServiceUnavailableException("Product Service is unreachable at " + productServiceUrl, ex);
        } catch (Exception ex) {
            throw new ProductServiceUnavailableException("Failed to communicate with Product Service: " + ex.getMessage(), ex);
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
