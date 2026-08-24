package com.bookecommerce.product_service.exception;

public record ApiError(boolean success, String message, String errorCode) {
}