package com.bookecommerce.order_service.client;

import com.bookecommerce.order_service.client.dto.CartResponseDto;
import com.bookecommerce.order_service.exception.CartNotFoundException;
import com.bookecommerce.order_service.exception.CartServiceUnavailableException;
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
public class CartServiceClient {

    private final RestClient restClient;
    private final String cartServiceUrl;

    public CartServiceClient(
            RestClient restClient,
            @Value("${services.cart-service.url:http://localhost:8084}") String cartServiceUrl) {
        this.restClient = restClient;
        this.cartServiceUrl = cartServiceUrl;
    }

    public CartResponseDto getActiveCart(UUID userId) {
        return getActiveCart(userId, null);
    }

    public CartResponseDto getActiveCart(UUID userId, String explicitAuthHeader) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        String authHeader = (explicitAuthHeader != null && !explicitAuthHeader.isBlank())
                ? explicitAuthHeader
                : getIncomingAuthorizationHeader();

        try {
            RestClient.RequestHeadersSpec<?> requestSpec = restClient.get()
                    .uri(cartServiceUrl + "/api/carts/{userId}/active", userId);

            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            CartResponseDto response = requestSpec
                    .retrieve()
                    .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), (req, resp) -> {
                        throw new CartNotFoundException(userId);
                    })
                    .onStatus(status -> status.equals(HttpStatus.UNAUTHORIZED) || status.equals(HttpStatus.FORBIDDEN), (req, resp) -> {
                        throw new UserUnauthorizedException("Authentication required by Cart Service");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new CartServiceUnavailableException("Cart Service returned server error");
                    })
                    .body(CartResponseDto.class);

            if (response == null) {
                throw new CartNotFoundException(userId);
            }

            return response;
        } catch (CartNotFoundException | UserUnauthorizedException | CartServiceUnavailableException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CartNotFoundException(userId);
            } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new UserUnauthorizedException("Authentication required by Cart Service");
            }
            throw new CartServiceUnavailableException("Cart Service error: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            throw new CartServiceUnavailableException("Cart Service is unreachable at " + cartServiceUrl, ex);
        } catch (Exception ex) {
            throw new CartServiceUnavailableException("Failed to communicate with Cart Service: " + ex.getMessage(), ex);
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
