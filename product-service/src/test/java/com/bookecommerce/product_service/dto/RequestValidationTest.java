package com.bookecommerce.product_service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class RequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void negativeBookPriceIsInvalid() {
        var request = new BookCreateRequest("isbn", "title", null, null, null, new java.math.BigDecimal("-1"));

        assertThat(validator.validate(request)).extracting("propertyPath").extracting(Object::toString)
                .contains("authorId", "categoryId", "price");
    }

    @Test
    void negativeListingFeeAndDurationAreInvalid() {
        var request = new BookListingCreateRequest(null, null, null, new java.math.BigDecimal("-1"), 0, null);

        assertThat(validator.validate(request)).extracting("propertyPath").extracting(Object::toString)
                .contains("bookId", "ownerId", "condition", "borrowFee", "borrowDurationDays");
    }
}