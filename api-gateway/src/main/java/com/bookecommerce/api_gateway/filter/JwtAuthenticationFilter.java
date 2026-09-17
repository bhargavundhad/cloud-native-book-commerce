package com.bookecommerce.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RestClient restClient;

    public JwtAuthenticationFilter(RestClient.Builder restClientBuilder) {

        this.restClient = restClientBuilder
                .baseUrl("http://localhost:8081")
                .build();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        /*
         * Public endpoints
         */
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * Get Authorization header
         */
        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        /*
         * JWT is required
         */
        if (authorization == null ||
                authorization.isBlank() ||
                !authorization.startsWith("Bearer ")) {

            sendUnauthorized(
                    response,
                    "Missing or invalid Authorization header"
            );

            return;
        }

        /*
         * Validate JWT with User Service
         */
        try {

            var validationResponse = restClient
                    .get()
                    .uri("/api/auth/validate")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            authorization
                    )
                    .exchange((requestMessage, clientResponse) -> {

                        HttpStatusCode status =
                                clientResponse.getStatusCode();

                        return status;
                    });

            /*
             * User Service accepted JWT
             */
            if (validationResponse.is2xxSuccessful()) {

                /*
                 * JWT is already present in the original request.
                 *
                 * Gateway will forward the Authorization header
                 * to the downstream service.
                 */
                filterChain.doFilter(request, response);

                return;
            }

            /*
             * User Service rejected JWT
             */
            if (validationResponse.value() == 401) {

                sendUnauthorized(
                        response,
                        "Invalid or expired JWT"
                );

                return;
            }

            if (validationResponse.value() == 403) {

                sendForbidden(
                        response,
                        "Access denied"
                );

                return;
            }

            /*
             * Unexpected response from User Service
             */
            sendUnauthorized(
                    response,
                    "Authentication failed"
            );

        } catch (Exception exception) {

            /*
             * User Service unavailable
             */
            sendServiceUnavailable(
                    response,
                    "Authentication service unavailable"
            );
        }
    }


    /*
     * Public endpoints
     */
    private boolean isPublicEndpoint(String path) {

        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/validate")
                || path.startsWith("/actuator");
    }


    /*
     * 401 Unauthorized
     */
    private void sendUnauthorized(
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        response.getWriter().write(
                """
                {
                    "success": false,
                    "message": "%s"
                }
                """.formatted(message)
        );
    }


    /*
     * 403 Forbidden
     */
    private void sendForbidden(
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        response.getWriter().write(
                """
                {
                    "success": false,
                    "message": "%s"
                }
                """.formatted(message)
        );
    }


    /*
     * 503 Service Unavailable
     */
    private void sendServiceUnavailable(
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE
        );

        response.setContentType("application/json");

        response.getWriter().write(
                """
                {
                    "success": false,
                    "message": "%s"
                }
                """.formatted(message)
        );
    }
}