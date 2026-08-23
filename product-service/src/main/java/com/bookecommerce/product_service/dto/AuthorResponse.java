package com.bookecommerce.product_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthorResponse(UUID id, String name, String biography, LocalDateTime createdAt) {
}