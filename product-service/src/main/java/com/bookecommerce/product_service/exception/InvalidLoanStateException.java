package com.bookecommerce.product_service.exception;

public class InvalidLoanStateException extends ConflictException {
    public InvalidLoanStateException(String message) {
        super(message, "INVALID_LOAN_STATE");
    }
}