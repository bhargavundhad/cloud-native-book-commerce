package com.bookecommerce.product_service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record LoanCreateRequest(@NotNull UUID listingId, @NotNull UUID borrowerId) {
}