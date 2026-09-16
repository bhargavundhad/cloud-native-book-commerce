package com.bookecommerce.cart_service.integration.user;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final String internalServiceSecret;

    public UserServiceClient(
            @Value("${app.service-urls.user}") String userServiceBaseUrl,
            @Value("${app.internal.service-secret}") String internalServiceSecret) {
        this.restClient = RestClient.builder()
                .baseUrl(userServiceBaseUrl)
                .build();
        this.internalServiceSecret = internalServiceSecret;
    }

    public boolean userExists(UUID userId) {
        try {
            UserExistsResponse response = restClient.get()
                    .uri("/api/internal/users/{userId}/exists", userId)
                    .header("X-Internal-Service-Secret", internalServiceSecret)
                    .retrieve()
                    .body(UserExistsResponse.class);

            if (response == null) {
                throw new UserServiceIntegrationException(
                        "User service returned an unexpected response for user: " + userId,
                        "USER_SERVICE_UNAVAILABLE");
            }

            return response.exists();
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new UserServiceIntegrationException("User service rejected the internal service request", "USER_SERVICE_UNAUTHORIZED");
        } catch (HttpClientErrorException.NotFound exception) {
            throw new UserServiceNotFoundException("User not found for cart operation: " + userId, "USER_NOT_FOUND");
        } catch (HttpClientErrorException.BadRequest exception) {
            throw new UserServiceIntegrationException("Invalid user identifier: " + userId, "INVALID_USER_ID");
        } catch (HttpServerErrorException exception) {
            throw new UserServiceIntegrationException("User service unavailable for user: " + userId,
                    "USER_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException exception) {
            throw new UserServiceIntegrationException("User service unavailable for user: " + userId,
                    "USER_SERVICE_UNAVAILABLE");
        }
    }
}
