package com.bookecommerce.cart_service.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ApiError> handleCartNotFound(CartNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "CART_NOT_FOUND");
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<ApiError> handleCartItemNotFound(CartItemNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "CART_ITEM_NOT_FOUND");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ApiError(false, "Request validation failed", "VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String errorCode = exception.getRequiredType() == UUID.class ? "INVALID_UUID" : "INVALID_PARAMETER";
        String message = exception.getRequiredType() == UUID.class
                ? "Invalid UUID: " + exception.getValue()
                : "Invalid request parameter";
        return error(HttpStatus.BAD_REQUEST, message, errorCode);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request", "INVALID_REQUEST");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDatabaseConflict(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "The request conflicts with existing data", "DATABASE_CONFLICT");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, String errorCode) {
        return ResponseEntity.status(status).body(new ApiError(false, message, errorCode));
    }
}
