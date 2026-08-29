package com.bookecommerce.user_service.exception;

import com.bookecommerce.user_service.dto.common.ApiErrorResponse;
import com.fasterxml.jackson.core.JsonParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================
    // 400 - VALIDATION ERROR
    // ==========================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .orElse("Validation failed");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                "VALIDATION_ERROR"
        );
    }


    // ==========================================
    // 400 - MALFORMED JSON
    // ==========================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException ex
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed request body",
                "MALFORMED_REQUEST"
        );
    }


    // ==========================================
    // 400 - ILLEGAL ARGUMENT
    // ==========================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "INVALID_REQUEST"
        );
    }


    // ==========================================
    // 401 - BAD CREDENTIALS
    // ==========================================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex
    ) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                "INVALID_CREDENTIALS"
        );
    }


    // ==========================================
    // 404 - USER NOT FOUND
    // ==========================================

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UserNotFoundException ex
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                "USER_NOT_FOUND"
        );
    }


    // ==========================================
    // 409 - DUPLICATE EMAIL
    // ==========================================

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "EMAIL_ALREADY_REGISTERED"
        );
    }


    // ==========================================
    // 409 - DUPLICATE PHONE
    // ==========================================

    @ExceptionHandler(DuplicatePhoneException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicatePhone(
            DuplicatePhoneException ex
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "PHONE_ALREADY_REGISTERED"
        );
    }


    // ==========================================
    // 500 - UNEXPECTED ERROR
    // ==========================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex
    ) {

        // Do NOT return ex.getMessage().
        // It could expose SQL/internal implementation details.

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                "INTERNAL_SERVER_ERROR"
        );
    }


    // ==========================================
    // COMMON RESPONSE BUILDER
    // ==========================================

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String errorCode
    ) {

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .success(false)
                        .message(message)
                        .errorCode(errorCode)
                        .build();

        return ResponseEntity
                .status(status)
                .body(response);
    }
}