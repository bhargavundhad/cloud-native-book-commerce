package com.bookecommerce.inventory_service.exception;

public record ApiError(boolean success, String message, String errorCode) {
}
