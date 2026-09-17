package com.bookecommerce.order_service.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(OrderNotFoundException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "ORDER_NOT_FOUND"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = Map.of(
                "success", false,
                "message", "Request validation failed",
                "errorCode", "VALIDATION_ERROR",
                "errors", errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "Invalid parameter: " + ex.getName();
        if (ex.getRequiredType() == UUID.class) {
            msg = "Invalid UUID format: " + ex.getValue();
        }
        Map<String, Object> body = Map.of(
                "success", false,
                "message", msg,
                "errorCode", "INVALID_PARAMETER"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "USER_NOT_FOUND"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleUserServiceUnavailable(UserServiceUnavailableException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "USER_SERVICE_UNAVAILABLE"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(UserUnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUserUnauthorized(UserUnauthorizedException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "UNAUTHORIZED"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(UserForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleUserForbidden(UserForbiddenException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "FORBIDDEN"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCartNotFound(CartNotFoundException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "CART_NOT_FOUND"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(CartEmptyException.class)
    public ResponseEntity<Map<String, Object>> handleCartEmpty(CartEmptyException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "CART_EMPTY"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(CartNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleCartNotActive(CartNotActiveException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "CART_NOT_ACTIVE"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(CartServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleCartServiceUnavailable(CartServiceUnavailableException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "CART_SERVICE_UNAVAILABLE"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFound(ProductNotFoundException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "PRODUCT_NOT_FOUND"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ProductServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleProductServiceUnavailable(ProductServiceUnavailableException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "PRODUCT_SERVICE_UNAVAILABLE"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(InsufficientStockException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "INSUFFICIENT_STOCK"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInventoryNotFound(InventoryNotFoundException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "INVENTORY_NOT_FOUND"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(InventoryServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleInventoryServiceUnavailable(InventoryServiceUnavailableException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "INVENTORY_SERVICE_UNAVAILABLE"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentFailed(PaymentFailedException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "PAYMENT_FAILED"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(PaymentServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentServiceUnavailable(PaymentServiceUnavailableException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage(),
                "errorCode", "PAYMENT_SERVICE_UNAVAILABLE"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        Map<String, Object> body = Map.of(
                "success", false,
                "message", ex.getMessage() != null ? ex.getMessage() : "Bad request",
                "errorCode", "BAD_REQUEST"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
