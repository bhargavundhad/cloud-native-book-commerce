package com.bookecommerce.product_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, String description, LocalDateTime createdAt) {
}