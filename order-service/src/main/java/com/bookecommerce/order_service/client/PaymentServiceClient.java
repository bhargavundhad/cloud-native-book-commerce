package com.bookecommerce.order_service.client;

import com.bookecommerce.order_service.client.dto.PaymentRequestDto;
import com.bookecommerce.order_service.client.dto.PaymentResponseDto;
import com.bookecommerce.order_service.exception.PaymentFailedException;
import com.bookecommerce.order_service.exception.PaymentServiceUnavailableException;
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

@Component
public class PaymentServiceClient {

    private final RestClient restClient;
    private final String paymentServiceUrl;

    public PaymentServiceClient(
            RestClient restClient,
            @Value("${services.payment-service.url:http://localhost:8086}") String paymentServiceUrl) {
        this.restClient = restClient;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    public PaymentResponseDto createPayment(PaymentRequestDto requestDto) {
        return createPayment(requestDto, null);
    }

    public PaymentResponseDto createPayment(PaymentRequestDto requestDto, String explicitAuthHeader) {
        if (requestDto == null) {
            throw new IllegalArgumentException("PaymentRequestDto cannot be null");
        }

        String authHeader = (explicitAuthHeader != null && !explicitAuthHeader.isBlank())
                ? explicitAuthHeader
                : getIncomingAuthorizationHeader();

        try {
            RestClient.RequestBodySpec requestSpec = restClient.post()
                    .uri(paymentServiceUrl + "/api/payments")
                    .body(requestDto);

            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            PaymentResponseDto response = requestSpec
                    .retrieve()
                    .onStatus(status -> status.equals(HttpStatus.BAD_REQUEST), (req, resp) -> {
                        throw new PaymentFailedException("Payment failed for order: " + requestDto.referenceId());
                    })
                    .onStatus(status -> status.equals(HttpStatus.UNAUTHORIZED) || status.equals(HttpStatus.FORBIDDEN), (req, resp) -> {
                        throw new UserUnauthorizedException("Authentication required by Payment Service");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new PaymentServiceUnavailableException("Payment Service returned server error");
                    })
                    .body(PaymentResponseDto.class);

            if (response == null) {
                throw new PaymentFailedException("Received null response from Payment Service");
            }

            return response;
        } catch (PaymentFailedException | UserUnauthorizedException | PaymentServiceUnavailableException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new PaymentFailedException("Payment failed for order: " + requestDto.referenceId());
            } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new UserUnauthorizedException("Authentication required by Payment Service");
            }
            throw new PaymentServiceUnavailableException("Payment Service error: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            throw new PaymentServiceUnavailableException("Payment Service is unreachable at " + paymentServiceUrl, ex);
        } catch (Exception ex) {
            throw new PaymentServiceUnavailableException("Failed to communicate with Payment Service: " + ex.getMessage(), ex);
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
