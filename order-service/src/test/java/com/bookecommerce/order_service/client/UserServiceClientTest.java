package com.bookecommerce.order_service.client;

import com.bookecommerce.order_service.client.dto.UserResponseDto;
import com.bookecommerce.order_service.exception.UserNotFoundException;
import com.bookecommerce.order_service.exception.UserServiceUnavailableException;
import com.bookecommerce.order_service.exception.UserUnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class UserServiceClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private UserServiceClient userServiceClient;
    private static final String BASE_URL = "http://localhost:8081";

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        userServiceClient = new UserServiceClient(restClientBuilder.build(), BASE_URL);
    }

    @Test
    @DisplayName("Should forward Authorization header and return UserResponseDto when User Service responds with 200 OK")
    void testVerifyUserExistsSuccessWithBearerToken() {
        UUID userId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";
        String jsonResponse = """
                {
                    "success": true,
                    "message": "User fetched successfully",
                    "data": {
                        "id": "%s",
                        "firstName": "John",
                        "lastName": "Doe",
                        "email": "john@example.com",
                        "role": "CUSTOMER",
                        "isActive": true
                    }
                }
                """.formatted(userId);

        mockServer.expect(requestTo(BASE_URL + "/api/users/" + userId))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        UserResponseDto dto = userServiceClient.verifyUserExists(userId, bearerToken);

        assertNotNull(dto);
        assertEquals(userId, dto.id());
        assertEquals("john@example.com", dto.email());
        assertTrue(dto.isActive());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when User Service returns 404 NOT_FOUND")
    void testVerifyUserExistsNotFound() {
        UUID userId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";
        String jsonResponse = """
                {
                    "success": false,
                    "message": "User not found with id: %s",
                    "errorCode": "USER_NOT_FOUND"
                }
                """.formatted(userId);

        mockServer.expect(requestTo(BASE_URL + "/api/users/" + userId))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(jsonResponse));

        assertThrows(UserNotFoundException.class, () -> userServiceClient.verifyUserExists(userId, bearerToken));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw UserUnauthorizedException when User Service returns 401 UNAUTHORIZED for missing/invalid JWT")
    void testVerifyUserExistsUnauthorized() {
        UUID userId = UUID.randomUUID();
        String jsonResponse = """
                {
                    "success": false,
                    "message": "Authentication required",
                    "errorCode": "UNAUTHORIZED"
                }
                """.formatted(userId);

        mockServer.expect(requestTo(BASE_URL + "/api/users/" + userId))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body(jsonResponse));

        assertThrows(UserUnauthorizedException.class, () -> userServiceClient.verifyUserExists(userId, null));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw UserServiceUnavailableException when User Service returns 500 Internal Server Error")
    void testVerifyUserExistsServerError() {
        UUID userId = UUID.randomUUID();
        String bearerToken = "Bearer test-jwt-token";

        mockServer.expect(requestTo(BASE_URL + "/api/users/" + userId))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andRespond(withServerError());

        assertThrows(UserServiceUnavailableException.class, () -> userServiceClient.verifyUserExists(userId, bearerToken));
        mockServer.verify();
    }
}
