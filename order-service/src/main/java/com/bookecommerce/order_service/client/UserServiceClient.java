package com.bookecommerce.order_service.client;

import com.bookecommerce.order_service.client.dto.UserResponseDto;
import com.bookecommerce.order_service.client.dto.UserServiceApiResponse;
import com.bookecommerce.order_service.exception.UserNotFoundException;
import com.bookecommerce.order_service.exception.UserServiceUnavailableException;
import com.bookecommerce.order_service.exception.UserUnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
public class UserServiceClient {

    private final RestClient restClient;
    private final String userServiceUrl;

    public UserServiceClient(
            RestClient restClient,
            @Value("${services.user-service.url:http://localhost:8081}") String userServiceUrl) {
        this.restClient = restClient;
        this.userServiceUrl = userServiceUrl;
    }

    public UserResponseDto verifyUserExists(UUID userId) {
        return verifyUserExists(userId, null);
    }

    public UserResponseDto verifyUserExists(UUID userId, String explicitAuthHeader) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        String authHeader = (explicitAuthHeader != null && !explicitAuthHeader.isBlank())
                ? explicitAuthHeader
                : getIncomingAuthorizationHeader();

        try {
            RestClient.RequestHeadersSpec<?> requestSpec = restClient.get()
                    .uri(userServiceUrl + "/api/users/{id}", userId);

            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            UserServiceApiResponse<UserResponseDto> response = requestSpec
                    .retrieve()
                    .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), (req, resp) -> {
                        throw new UserNotFoundException(userId);
                    })
                    .onStatus(status -> status.equals(HttpStatus.UNAUTHORIZED) || status.equals(HttpStatus.FORBIDDEN), (req, resp) -> {
                        throw new UserUnauthorizedException("Authentication required by User Service");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new UserServiceUnavailableException("User Service returned server error");
                    })
                    .body(new ParameterizedTypeReference<UserServiceApiResponse<UserResponseDto>>() {});

            if (response == null || !response.success() || response.data() == null) {
                throw new UserNotFoundException(userId);
            }

            if (Boolean.FALSE.equals(response.data().isActive())) {
                throw new UserNotFoundException("User account is inactive for id: " + userId);
            }

            return response.data();
        } catch (UserNotFoundException | UserUnauthorizedException | UserServiceUnavailableException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new UserNotFoundException(userId);
            } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new UserUnauthorizedException("Authentication required by User Service");
            }
            throw new UserServiceUnavailableException("User Service error: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            throw new UserServiceUnavailableException("User Service is unreachable at " + userServiceUrl, ex);
        } catch (Exception ex) {
            throw new UserServiceUnavailableException("Failed to communicate with User Service: " + ex.getMessage(), ex);
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
